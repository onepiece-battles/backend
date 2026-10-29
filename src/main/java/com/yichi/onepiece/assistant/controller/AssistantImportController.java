package com.yichi.onepiece.assistant.controller;

import com.yichi.onepiece.assistant.agent.AssistantCancelledException;
import com.yichi.onepiece.assistant.agent.AssistantException;
import com.yichi.onepiece.assistant.agent.AssistantExecutors;
import com.yichi.onepiece.assistant.agent.AssistantLanguage;
import com.yichi.onepiece.assistant.agent.AssistantRateLimiter;
import com.yichi.onepiece.assistant.agent.AssistantRateLimiter.Decision;
import com.yichi.onepiece.assistant.importing.ImportCommitter;
import com.yichi.onepiece.assistant.importing.ImportCommitter.ImportRejected;
import com.yichi.onepiece.assistant.importing.ImportDrafter;
import com.yichi.onepiece.assistant.importing.ImportDto.CommitRejected;
import com.yichi.onepiece.assistant.importing.ImportDto.CommitRequest;
import com.yichi.onepiece.assistant.importing.ImportDto.DraftRequest;
import com.yichi.onepiece.assistant.importing.ImportDto.ReviseRequest;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.model.UserDetailImpl;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI batch import (NIKA only). {@code /draft} streams progress as {@code status} events and ends
 * with one {@code draft} (or {@code error}); {@code /revise} changes a draft the same way, as the
 * reviewer asks. Nothing is written until the reviewed draft is sent to {@code /commit}.
 */
@RestController
@RequestMapping("/api/onepiece/assistant/import")
public class AssistantImportController {
    private static final Logger logger = LoggerFactory.getLogger(AssistantImportController.class);
    // Several DeepSeek calls; progress events keep nginx's 300 s read timeout from firing.
    private static final Duration DRAFT_TIMEOUT = Duration.ofSeconds(280);
    static final int MAX_SCOPE_LENGTH = 500;

    private final ImportDrafter drafter;
    private final ImportCommitter committer;
    private final AssistantRateLimiter rateLimiter;
    private final AssistantExecutors executors;

    public AssistantImportController(ImportDrafter drafter, ImportCommitter committer,
            AssistantRateLimiter rateLimiter, AssistantExecutors executors) {
        this.drafter = drafter;
        this.committer = committer;
        this.rateLimiter = rateLimiter;
        this.executors = executors;
    }

    @PostMapping(value = "/draft", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter draft(@RequestBody DraftRequest request, HttpServletResponse response) {
        requireNika();
        UserDetailImpl user = UserHelper.getCurrentUserDetail();
        AssistantLanguage language = AssistantLanguage.resolve(request.language(), user.getLocale());
        String scope = request.scope() == null ? "" : request.scope().strip();
        String invalid = scope.isEmpty() || scope.length() > MAX_SCOPE_LENGTH
                ? language.pick(
                        "请描述要录入的范围,不超过 " + MAX_SCOPE_LENGTH + " 字。",
                        "Describe what to import in at most " + MAX_SCOPE_LENGTH + " characters.")
                : null;
        return stream(response, user.getId(), language, invalid,
                progress -> drafter.draft(scope, user.getId(), language, progress));
    }

    /** Changes the reviewed draft as the reviewer asks; streams like {@code /draft}. */
    @PostMapping(value = "/revise", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter revise(@RequestBody ReviseRequest request, HttpServletResponse response) {
        requireNika();
        UserDetailImpl user = UserHelper.getCurrentUserDetail();
        AssistantLanguage language = AssistantLanguage.resolve(request.language(), user.getLocale());
        String instruction = request.instruction() == null ? "" : request.instruction().strip();
        String invalid = instruction.isEmpty() || instruction.length() > MAX_SCOPE_LENGTH
                ? language.pick(
                        "请说明要怎么修改,不超过 " + MAX_SCOPE_LENGTH + " 字。",
                        "Say what to change in at most " + MAX_SCOPE_LENGTH + " characters.")
                : request.items() == null || request.items().isEmpty()
                                || request.items().size() > ImportCommitter.MAX_ITEMS
                        ? language.pick("草稿为空或太大。", "The draft is empty or too large.")
                        : null;
        return stream(response, user.getId(), language, invalid,
                progress -> drafter.revise(
                        request.items(), request.messages(), instruction, user.getId(), language, progress));
    }

    /**
     * Runs {@code job} on the assistant's worker pool and streams its progress as {@code status}
     * events, then the result as one {@code draft} event, or an {@code error}. {@code invalid} is the
     * message for a request that cannot run.
     */
    private SseEmitter stream(HttpServletResponse response, UUID userId, AssistantLanguage language,
            String invalid, Function<Consumer<String>, Object> job) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        SseEmitter emitter = new SseEmitter(DRAFT_TIMEOUT.toMillis());
        SseEventSink sink = new SseEventSink(emitter);
        emitter.onCompletion(sink::markClosed);
        emitter.onError(error -> sink.markClosed());
        emitter.onTimeout(() -> sink.error(AssistantException.TIMEOUT,
                language.pick("起草超时了,请缩小范围后重试。", "Drafting took too long. Try a smaller scope.")));
        if (invalid != null) {
            sink.error(AssistantException.INVALID_REQUEST, invalid);
            return emitter;
        }
        // Shares the assistant's limiter: one heavy DeepSeek job per user at a time.
        Decision decision = rateLimiter.tryAcquire(userId);
        if (decision != Decision.ACQUIRED) {
            sink.error(decision == Decision.BUSY ? AssistantException.BUSY : AssistantException.RATE_LIMITED,
                    AssistantController.rejection(decision, language));
            return emitter;
        }

        Runnable work = () -> {
            try {
                sink.finish("draft", job.apply(label -> sink.status("import", label)));
            } catch (AssistantCancelledException e) {
                logger.debug("import draft for user {} was closed by the client", userId);
            } catch (AssistantException e) {
                logger.warn("import draft for user {} failed: {} {}", userId, e.code(), e.getMessage());
                sink.error(e.code(), AssistantController.userMessage(e, language));
            } catch (RuntimeException e) {
                logger.error("import draft for user {} failed", userId, e);
                sink.error(AssistantException.INTERNAL_ERROR,
                        language.pick("起草出错了,请稍后再试。", "Drafting failed. Please try again later."));
            } finally {
                rateLimiter.release(userId);
            }
        };
        try {
            // The worker needs the caller's security context for the permission checks of the lookups.
            executors.turns().execute(new DelegatingSecurityContextRunnable(work, SecurityContextHolder.getContext()));
        } catch (RejectedExecutionException e) {
            rateLimiter.release(userId);
            sink.error(AssistantException.BUSY, AssistantController.rejection(Decision.BUSY, language));
        }
        return emitter;
    }

    /** Imports the reviewed items; a rejected batch answers 422 with every problem found. */
    @PostMapping("/commit")
    public ResponseEntity<?> commit(@RequestBody CommitRequest request) {
        requireNika();
        UserDetailImpl user = UserHelper.getCurrentUserDetail();
        AssistantLanguage language = AssistantLanguage.resolve(request.language(), user.getLocale());
        try {
            return ResponseEntity.ok(
                    committer.commit(request.items(), user.getId(), user.getLoginRoleId(), language));
        } catch (ImportRejected e) {
            return ResponseEntity.unprocessableEntity().body(new CommitRejected("invalid_import",
                    language.pick("有内容需要修改,未导入任何数据。", "Nothing was imported; fix the listed problems."),
                    e.problems()));
        }
    }

    private static void requireNika() {
        if (!UserHelper.hasNikaPermission()) {
            throw new PermissionDeniedException("Access Denied");
        }
    }
}
