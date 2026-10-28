package com.yichi.onepiece.assistant.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient.Completion;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient.CompletionRequest;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient.ToolCall;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient.Usage;
import com.yichi.onepiece.assistant.dto.AssistantDto.BattleProposal;
import com.yichi.onepiece.assistant.dto.AssistantDto.DoneEvent;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.assistant.service.AiAssistantService;
import com.yichi.onepiece.assistant.service.AiSettingService;
import com.yichi.onepiece.assistant.service.AiTurn;
import com.yichi.onepiece.assistant.tools.ActionConfirmationService;
import com.yichi.onepiece.assistant.tools.ActionSchemas;
import com.yichi.onepiece.exception.PermissionDeniedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * The server-side assistant: owns the system prompt, the tools and the
 * tool-calling loop, so the client only sends a question and renders events.
 */
@Service
public class AssistantAgent {
    private static final Logger logger = LoggerFactory.getLogger(AssistantAgent.class);
    static final int MAX_TOOL_ROUNDS = 8;
    static final double TEMPERATURE = 0.3;
    static final int MAX_TOKENS = 2000;
    static final String DEFAULT_MODEL = "deepseek-chat";
    // Models known to support function calling; any other would silently lose the tools.
    static final Set<String> TOOL_CAPABLE_MODELS = Set.of(DEFAULT_MODEL);
    static final String BATTLE_PROPOSAL = "battle_proposal";

    private final AiSettingService settingService;
    private final AiAssistantService assistantService;
    private final ChatCompletionClient client;
    private final ToolRegistry toolRegistry;
    private final Executor toolExecutor;
    private final ActionConfirmationService actions;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AssistantAgent(AiSettingService settingService, AiAssistantService assistantService,
            ChatCompletionClient client, ToolRegistry toolRegistry, AssistantExecutors executors, ActionConfirmationService actions) {
        this(settingService, assistantService, client, toolRegistry, executors.tools(), actions);
    }

    AssistantAgent(AiSettingService settingService, AiAssistantService assistantService,
            ChatCompletionClient client, ToolRegistry toolRegistry, Executor toolExecutor) {
        this(settingService, assistantService, client, toolRegistry, toolExecutor, null);
    }

    AssistantAgent(AiSettingService settingService, AiAssistantService assistantService,
            ChatCompletionClient client, ToolRegistry toolRegistry, Executor toolExecutor, ActionConfirmationService actions) {
        this.actions = actions;
        this.settingService = settingService;
        this.assistantService = assistantService;
        this.client = client;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
    }

    /**
     * Answers one question for {@code userId}: replays recent context, lets the
     * model call tools for up to {@value #MAX_TOOL_ROUNDS} rounds, streams the
     * answer to {@code sink} and persists the whole turn. Must run on a thread
     * that carries the caller's security context.
     */
    public void run(UUID userId, String question, AssistantLanguage language, AssistantEventSink sink) {
        ToolContext context = new ToolContext(userId, language);
        if (actions != null) {
            Optional<ActionConfirmationService.Reply> reply;
            try {
                reply = actions.handle(question, context);
            } catch (ToolProblem problem) {
                reply = Optional.of(new ActionConfirmationService.Reply(language.pick(
                        "未执行操作：", "No change executed: ") + problem.getMessage(), problem.result().resources()));
            } catch (PermissionDeniedException denied) {
                reply = Optional.of(new ActionConfirmationService.Reply(language.pick(
                        "当前角色没有执行此操作的权限。", "The current role cannot perform this action."), List.of()));
            }
            if (reply.isPresent()) {
                var confirmed = reply.get();
                List<String> ids = assistantService.saveTurns(userId, List.of(AiTurn.user(question), AiTurn.assistant(confirmed.content(), confirmed.resources())));
                sink.delta(confirmed.content());
                sink.done(new DoneEvent(ids.get(ids.size()-1), confirmed.content(), confirmed.resources(), null));
                return;
            }
        }
        if (!settingService.isConfigured()) {
            throw new AssistantException(AssistantException.NOT_CONFIGURED, language.pick(
                    "AI 助手还没有配置 DeepSeek Key,请联系管理员。",
                    "The assistant has no DeepSeek key configured yet."));
        }
        long startedAt = System.nanoTime();
        Settings settings = new Settings(
                settingService.baseUrl(), settingService.apiKey(), resolveModel(settingService.model()));
        ConversationReplay.Replay replay =
                ConversationReplay.build(assistantService.recentTurns(userId), objectMapper);
        // Persist the question first so it survives even if the answer fails.
        assistantService.saveTurns(userId, List.of(AiTurn.user(question)));

        List<ObjectNode> transcript = new ArrayList<>();
        transcript.add(Transcript.message(objectMapper, "system", AssistantPrompt.SYSTEM));
        transcript.addAll(replay.messages());
        transcript.add(Transcript.message(objectMapper, AiTurn.USER, question));

        Set<ResourceLink> mentioned = new LinkedHashSet<>();
        List<AiTurn> toolTurns = new ArrayList<>();
        List<String> toolsUsed = new ArrayList<>();
        Usage usage = Usage.NONE;
        int rounds = 0;
        String answer = null;

        try {
            while (answer == null && rounds < MAX_TOOL_ROUNDS) {
                rounds++;
                Completion completion = complete(settings, transcript, toolRegistry.definitions(), sink);
                usage = usage.plus(completion.usage());
                if (!completion.hasToolCalls()) {
                    answer = completion.content();
                    break;
                }
                ArrayNode calls = Transcript.toolCalls(objectMapper, completion.toolCalls());
                transcript.add(Transcript.assistantToolCalls(objectMapper, completion.content(), calls));
                toolTurns.add(AiTurn.assistantToolCalls(completion.content(), calls.toString()));
                List<ToolResult> results = runTools(completion.toolCalls(), context, sink);
                for (int i = 0; i < results.size(); i++) {
                    ToolCall call = completion.toolCalls().get(i);
                    ToolResult result = results.get(i);
                    transcript.add(Transcript.tool(objectMapper, call.id(), result.content()));
                    // Stored trimmed: replay never uses more, and it keeps the row within a TEXT column.
                    toolTurns.add(AiTurn.tool(
                            call.id(), ConversationReplay.trimToolContent(result.content()), result.resources()));
                    mentioned.addAll(result.resources());
                    toolsUsed.add(call.name());
                }
            }
            if (answer == null) {
                // Out of tool rounds: answer from what has been gathered, with tools disabled.
                rounds++;
                Completion completion = complete(settings, transcript, null, sink);
                usage = usage.plus(completion.usage());
                answer = completion.content();
            }
            if (answer == null || answer.isBlank()) {
                throw new AssistantException(AssistantException.UPSTREAM_ERROR, language.pick(
                        "DeepSeek 返回了空回答,请重试。", "DeepSeek returned an empty answer. Please try again."));
            }

            List<ResourceLink> known = new ArrayList<>(replay.resources());
            known.addAll(mentioned);
            String content = ResourceLinkNormalizer.normalize(answer, known);
            if (actions != null) {
                String instructions = actions.instructions(mentioned, context);
                content += instructions;
                if (!instructions.isEmpty()) sink.delta(instructions);
            }
            List<ResourceLink> links = mentioned.stream()
                    .filter(r -> !BATTLE_PROPOSAL.equals(r.type()) && !ActionConfirmationService.PROPOSAL.equals(r.type()) && r.id() != null && !r.id().isEmpty())
                    .toList();

            List<AiTurn> turns = new ArrayList<>(toolTurns);
            turns.add(AiTurn.assistant(content, links));
            List<String> ids = assistantService.saveTurns(userId, turns);
            if (actions != null) actions.publish(mentioned, context);
            sink.done(new DoneEvent(ids.get(ids.size() - 1), content, links, battleProposal(mentioned)));
            logger.info("assistant answered user {} in {} ms: {} round(s), tools {}, tokens {} in / {} out",
                    userId, (System.nanoTime() - startedAt) / 1_000_000, rounds, toolsUsed,
                    usage.promptTokens(), usage.completionTokens());
        } catch (RuntimeException failure) {
            if (actions != null) {
                try {
                    actions.discardUnpublished(mentioned, context);
                } catch (RuntimeException cleanupFailure) {
                    logger.warn("Could not discard unpublished assistant actions for {}", userId, cleanupFailure);
                }
            }
            throw failure;
        }
    }

    static String resolveModel(String configured) {
        String model = configured == null ? "" : configured.trim();
        if (TOOL_CAPABLE_MODELS.contains(model)) {
            return model;
        }
        logger.warn("Assistant model '{}' is not known to support tool calling; using {}",
                configured, DEFAULT_MODEL);
        return DEFAULT_MODEL;
    }

    private Completion complete(
            Settings settings, List<ObjectNode> transcript, ArrayNode tools, AssistantEventSink sink) {
        CompletionRequest request = new CompletionRequest(settings.baseUrl(), settings.apiKey(),
                settings.model(), List.copyOf(transcript), tools, TEMPERATURE, MAX_TOKENS);
        return client.complete(request, sink::delta);
    }

    /** Runs one round's calls in parallel; results come back in call order. */
    private List<ToolResult> runTools(List<ToolCall> calls, ToolContext context, AssistantEventSink sink) {
        List<JsonNode> arguments = calls.stream().map(call -> parseArguments(call.arguments())).toList();
        for (int i = 0; i < calls.size(); i++) {
            sink.status(calls.get(i).name(), label(calls.get(i), arguments.get(i), context.language()));
        }
        // Tool threads need the caller's security context: tools apply the same
        // permission checks and visibility rules as a direct API call.
        Executor executor = new DelegatingSecurityContextExecutor(toolExecutor, SecurityContextHolder.getContext());
        List<CompletableFuture<ToolResult>> futures = new ArrayList<>();
        for (int i = 0; i < calls.size(); i++) {
            ToolCall call = calls.get(i);
            JsonNode callArguments = arguments.get(i);
            CompletableFuture<ToolResult> future;
            try {
                future = CompletableFuture.supplyAsync(() -> runTool(call, callArguments, context), executor);
            } catch (RejectedExecutionException e) {
                future = CompletableFuture.completedFuture(failure("too many lookups at once"));
            }
            futures.add(future);
        }
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private ToolResult runTool(ToolCall call, JsonNode arguments, ToolContext context) {
        Optional<AssistantTool> tool = toolRegistry.find(call.name());
        if (tool.isEmpty()) {
            return failure("unknown tool " + call.name());
        }
        try {
            ActionSchemas.validate(arguments, tool.get().parameters(), "arguments");
            return tool.get().handler().execute(arguments, context);
        } catch (ToolProblem e) {
            return e.result();
        } catch (PermissionDeniedException e) {
            return failure("permission denied");
        } catch (RuntimeException e) {
            logger.warn("assistant tool {} failed", call.name(), e);
            return failure("lookup failed");
        }
    }

    private String label(ToolCall call, JsonNode arguments, AssistantLanguage language) {
        try {
            String label = toolRegistry.find(call.name())
                    .map(tool -> tool.labeler().label(arguments, language))
                    .orElse(null);
            return label == null || label.isBlank() ? call.name() : label;
        } catch (RuntimeException e) {
            return call.name();
        }
    }

    private JsonNode parseArguments(String raw) {
        if (raw == null || raw.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode node = objectMapper.readTree(raw);
            return node != null && node.isObject() ? node : objectMapper.createObjectNode();
        } catch (JsonProcessingException e) {
            return objectMapper.createObjectNode();
        }
    }

    private ToolResult failure(String reason) {
        return new ToolResult(objectMapper.createObjectNode().put("error", reason).toString(), List.of());
    }

    private static BattleProposal battleProposal(Collection<ResourceLink> resources) {
        for (ResourceLink resource : resources) {
            if (BATTLE_PROPOSAL.equals(resource.type())) {
                String[] ids = resource.id().split("\\|");
                if (ids.length == 2) {
                    return new BattleProposal(ids[0], ids[1], resource.name());
                }
            }
        }
        return null;
    }

    private record Settings(String baseUrl, String apiKey, String model) {}
}
