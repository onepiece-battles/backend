package com.yichi.onepiece.assistant.controller;

import com.yichi.onepiece.assistant.agent.AssistantAgent;
import com.yichi.onepiece.assistant.agent.ToolContext;
import com.yichi.onepiece.assistant.tools.ActionConfirmationService;
import com.yichi.onepiece.assistant.agent.AssistantCancelledException;
import com.yichi.onepiece.assistant.agent.AssistantException;
import com.yichi.onepiece.assistant.agent.AssistantExecutors;
import com.yichi.onepiece.assistant.agent.AssistantLanguage;
import com.yichi.onepiece.assistant.agent.AssistantRateLimiter;
import com.yichi.onepiece.assistant.agent.AssistantRateLimiter.Decision;
import com.yichi.onepiece.assistant.dto.AssistantDto.AiChatRequest;
import com.yichi.onepiece.assistant.dto.AssistantDto.AiChatResponse;
import com.yichi.onepiece.assistant.dto.AssistantDto.AiSettingRequest;
import com.yichi.onepiece.assistant.dto.AssistantDto.AiSettingResponse;
import com.yichi.onepiece.assistant.dto.AssistantDto.AskRequest;
import com.yichi.onepiece.assistant.dto.AssistantDto.ListAiMessagesResponse;
import com.yichi.onepiece.assistant.dto.AssistantDto.SaveAiMessageRequest;
import com.yichi.onepiece.assistant.dto.AssistantDto.SaveAiMessageResponse;
import com.yichi.onepiece.assistant.dto.AssistantDto.SimpleResult;
import com.yichi.onepiece.assistant.service.AiAssistantService;
import com.yichi.onepiece.assistant.service.AiSettingService;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.model.UserDetailImpl;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI assistant endpoints. All require a valid JWT (enforced by the security
 * chain). Provider-setting reads/writes additionally require the NIKA
 * super-admin; the DeepSeek key is never returned.
 */
@RestController
@RequestMapping("/api/onepiece/assistant")
public class AssistantController {
    private static final Logger logger = LoggerFactory.getLogger(AssistantController.class);
    // Below nginx's 300 s proxy_read_timeout, so the server always ends the stream first.
    private static final Duration ANSWER_TIMEOUT = Duration.ofSeconds(120);
    static final int MAX_QUESTION_LENGTH = 2000;

    private final AiAssistantService assistantService;
    private final AiSettingService settingService;
    private final AssistantAgent agent;
    private final AssistantRateLimiter rateLimiter;
    private final AssistantExecutors executors;
    private final ActionConfirmationService actions;

    public AssistantController(AiAssistantService assistantService, AiSettingService settingService,
            AssistantAgent agent, AssistantRateLimiter rateLimiter, AssistantExecutors executors, ActionConfirmationService actions) {
        this.assistantService = assistantService;
        this.settingService = settingService;
        this.agent = agent;
        this.rateLimiter = rateLimiter;
        this.executors = executors;
        this.actions = actions;
    }

    /**
     * Asks the server-side agent. The answer streams back as text/event-stream:
     * {@code status} while tools run, {@code delta} fragments of the answer, then
     * exactly one {@code done} (or {@code error}).
     */
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(@RequestBody AskRequest request, HttpServletResponse response) {
        // Without this nginx buffers the whole stream and the reply arrives all at once.
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        SseEmitter emitter = new SseEmitter(ANSWER_TIMEOUT.toMillis());
        SseEventSink sink = new SseEventSink(emitter);

        UserDetailImpl user = UserHelper.getCurrentUserDetail();
        UUID userId = user.getId();
        AssistantLanguage language = AssistantLanguage.resolve(request.language(), user.getLocale());
        emitter.onCompletion(sink::markClosed);
        emitter.onError(error -> sink.markClosed());
        emitter.onTimeout(() -> sink.error(AssistantException.TIMEOUT,
                language.pick("回答超时了,请重试。", "The answer took too long. Please try again.")));

        String question = request.message() == null ? "" : request.message().strip();
        if (question.isEmpty() || question.length() > MAX_QUESTION_LENGTH) {
            sink.error(AssistantException.INVALID_REQUEST, language.pick(
                    "问题不能为空,且不超过 " + MAX_QUESTION_LENGTH + " 字。",
                    "Ask a question of at most " + MAX_QUESTION_LENGTH + " characters."));
            return emitter;
        }
        Decision decision = rateLimiter.tryAcquire(userId);
        if (decision != Decision.ACQUIRED) {
            sink.error(decision == Decision.BUSY ? AssistantException.BUSY : AssistantException.RATE_LIMITED,
                    rejection(decision, language));
            return emitter;
        }

        Runnable answer = () -> {
            try {
                agent.run(userId, question, language, sink);
            } catch (AssistantCancelledException e) {
                logger.debug("assistant stream for user {} was closed by the client", userId);
            } catch (AssistantException e) {
                logger.warn("assistant answer for user {} failed: {} {}", userId, e.code(), e.getMessage());
                sink.error(e.code(), userMessage(e, language));
            } catch (RuntimeException e) {
                logger.error("assistant answer for user {} failed", userId, e);
                sink.error(AssistantException.INTERNAL_ERROR,
                        language.pick("助手出错了,请稍后再试。", "Something went wrong. Please try again later."));
            } finally {
                rateLimiter.release(userId);
            }
        };
        try {
            // The worker thread needs the caller's security context for the tools' permission checks.
            executors.turns().execute(new DelegatingSecurityContextRunnable(answer, SecurityContextHolder.getContext()));
        } catch (RejectedExecutionException e) {
            rateLimiter.release(userId);
            sink.error(AssistantException.BUSY, rejection(Decision.BUSY, language));
        }
        return emitter;
    }

    /**
     * Legacy pass-through of a client-built DeepSeek payload. Locked to NIKA so the
     * server key is no longer an open relay; removed once the client uses /ask.
     */
    @PostMapping("/chat")
    public AiChatResponse chat(@RequestBody AiChatRequest request) {
        requireNika();
        return assistantService.chat(request.requestBody());
    }

    @PostMapping("/list_messages")
    public ListAiMessagesResponse listMessages() {
        var user = UserHelper.getCurrentUserDetail();
        var context = new ToolContext(user.getId(), AssistantLanguage.resolve(null, user.getLocale()));
        return new ListAiMessagesResponse(actions.decorateMessages(assistantService.listMessages(user.getId()), context));
    }

    @PostMapping("/save_message")
    public SaveAiMessageResponse saveMessage(@RequestBody SaveAiMessageRequest request) {
        String id = assistantService.saveMessage(
                UserHelper.getCurrentUserId(), request.role(), request.content(), request.resources());
        return new SaveAiMessageResponse(id);
    }

    @PostMapping("/clear_messages")
    public SimpleResult clearMessages() {
        assistantService.clear(UserHelper.getCurrentUserId());
        return new SimpleResult(true);
    }

    @PostMapping("/get_setting")
    public AiSettingResponse getSetting() {
        requireNika();
        return new AiSettingResponse(
                settingService.isConfigured(), settingService.baseUrl(), settingService.model());
    }

    @PostMapping("/set_setting")
    public SimpleResult setSetting(@RequestBody AiSettingRequest request) {
        requireNika();
        settingService.save(request.apiKey(), request.baseUrl(), request.model());
        return new SimpleResult(true);
    }

    private void requireNika() {
        if (!UserHelper.hasNikaPermission()) {
            throw new PermissionDeniedException("Access Denied");
        }
    }

    private static String rejection(Decision decision, AssistantLanguage language) {
        return decision == Decision.RATE_LIMITED
                ? language.pick("提问太频繁了,请一分钟后再试。", "Too many questions. Please wait a minute.")
                : language.pick("上一个问题还在回答中,请稍等。", "Still answering your previous question.");
    }

    /** Upstream failures carry technical detail (already logged); show a plain message instead. */
    private static String userMessage(AssistantException e, AssistantLanguage language) {
        return switch (e.code()) {
            case AssistantException.TIMEOUT -> language.pick(
                    "DeepSeek 响应超时,请重试。", "DeepSeek did not respond in time. Please try again.");
            case AssistantException.UPSTREAM_ERROR -> language.pick(
                    "DeepSeek 服务出错了,请稍后再试。", "DeepSeek is unavailable right now. Please try again later.");
            default -> e.getMessage();
        };
    }
}
