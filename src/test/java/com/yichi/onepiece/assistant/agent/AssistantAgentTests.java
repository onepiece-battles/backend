package com.yichi.onepiece.assistant.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.assistant.tools.ActionConfirmationService;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AssistantAgentTests {
    private static final UUID USER = UUID.randomUUID();
    private static final ResourceLink LUFFY = new ResourceLink("character", "c-luffy", "Luffy");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AiSettingService settings;
    private AiAssistantService store;
    private ScriptedClient client;
    private RecordingSink sink;
    private List<AiTurn> saved;

    @BeforeMethod
    public void setUp() {
        settings = mock(AiSettingService.class);
        when(settings.isConfigured()).thenReturn(true);
        when(settings.baseUrl()).thenReturn("https://api.deepseek.test/chat/completions");
        when(settings.apiKey()).thenReturn("test-key");
        when(settings.model()).thenReturn("deepseek-chat");

        store = mock(AiAssistantService.class);
        when(store.recentTurns(USER)).thenReturn(List.of());
        saved = new ArrayList<>();
        AtomicInteger ids = new AtomicInteger();
        when(store.saveTurns(eq(USER), anyList())).thenAnswer(invocation -> {
            List<AiTurn> turns = invocation.getArgument(1);
            saved.addAll(turns);
            return turns.stream().map(turn -> "msg-" + ids.incrementAndGet()).toList();
        });

        client = new ScriptedClient();
        sink = new RecordingSink();
    }

    @Test
    public void answersAfterARoundOfToolCalls() {
        var agent = agent(tool("search_characters", new ToolResult("{\"characters\":[]}", List.of(LUFFY))));
        client.script.add(toolCall("call_1", "search_characters", "{\"name\":\"Luffy\"}"));
        client.script.add(answer("[Luffy](onepiece://character/c-luffy) is the captain."));

        agent.run(USER, "Who is Luffy?", AssistantLanguage.EN_US, sink);

        assertThat(sink.events.get(0)).isEqualTo("status:search_characters:running search_characters");
        assertThat(sink.streamed.toString()).isEqualTo("[Luffy](onepiece://character/c-luffy) is the captain.");
        assertThat(sink.done.content()).isEqualTo("[Luffy](onepiece://character/c-luffy) is the captain.");
        assertThat(sink.done.resources()).containsExactly(LUFFY);
        assertThat(sink.done.battleProposal()).isNull();

        // The second round sees the call and its result, with the tools still on offer.
        CompletionRequest second = client.requests.get(1);
        assertThat(roles(second.messages())).containsExactly("system", "user", "assistant", "tool");
        assertThat(second.messages().get(3).get("tool_call_id").asText()).isEqualTo("call_1");
        assertThat(second.tools()).isNotNull();

        // Question saved first, then the hidden tool round and the answer.
        assertThat(saved).extracting(AiTurn::role).containsExactly("user", "assistant", "tool", "assistant");
        assertThat(saved.get(1).hasToolCalls()).isTrue();
        assertThat(saved.get(2).resources()).containsExactly(LUFFY);
        assertThat(sink.done.messageId()).isEqualTo("msg-4");
    }

    @Test
    public void neverLinksAResourceNoToolReturned() {
        client.script.add(answer("[Zoro](onepiece://character/c-zoro) is strong."));

        agent().run(USER, "Is Zoro strong?", AssistantLanguage.EN_US, sink);

        assertThat(sink.done.content()).isEqualTo("Zoro is strong.");
    }

    @Test
    public void forcesAnAnswerWithoutToolsAfterTheRoundLimit() {
        var agent = agent(tool("search_characters", new ToolResult("{}", List.of())));
        client.forever = toolCall("call_x", "search_characters", "{}");
        client.whenToolsDisabled = answer("Here is what I found.");

        agent.run(USER, "Search everything", AssistantLanguage.EN_US, sink);

        assertThat(client.requests).hasSize(AssistantAgent.MAX_TOOL_ROUNDS + 1);
        assertThat(client.requests.get(AssistantAgent.MAX_TOOL_ROUNDS).tools()).isNull();
        assertThat(sink.done.content()).isEqualTo("Here is what I found.");
    }

    @Test
    public void reportsAnUnknownToolToTheModelAndKeepsGoing() {
        client.script.add(toolCall("call_1", "no_such_tool", "{}"));
        client.script.add(answer("Sorry, I could not look that up."));

        agent().run(USER, "?", AssistantLanguage.EN_US, sink);

        assertThat(client.requests.get(1).messages().get(3).get("content").asText()).contains("unknown tool");
        assertThat(sink.done.content()).isEqualTo("Sorry, I could not look that up.");
    }

    @Test
    public void turnsAPermissionDenialIntoAToolError() {
        var denied = new AssistantTool("get_group_detail", "test", schema(),
                (arguments, context) -> {
                    throw new PermissionDeniedException("Access Denied");
                },
                (arguments, language) -> "checking");
        client.script.add(toolCall("call_1", "get_group_detail", "{\"name\":\"Straw Hats\"}"));
        client.script.add(answer("You cannot see that crew."));

        agent(denied).run(USER, "Straw Hats?", AssistantLanguage.EN_US, sink);

        assertThat(client.requests.get(1).messages().get(3).get("content").asText()).contains("permission denied");
    }

    @Test
    public void offersTheBattleProposalSeparatelyFromTheLinks() {
        var law = new ResourceLink("character", "c-law", "Law");
        var proposal = new ResourceLink(AssistantAgent.BATTLE_PROPOSAL, "c-luffy|c-law", "Luffy vs Law");
        client.script.add(toolCall("call_1", "propose_battle", "{}"));
        client.script.add(answer("I prepared the battle below."));

        agent(tool("propose_battle", new ToolResult("{\"proposed\":true}", List.of(LUFFY, law, proposal))))
                .run(USER, "Luffy vs Law", AssistantLanguage.EN_US, sink);

        assertThat(sink.done.battleProposal()).isEqualTo(new BattleProposal("c-luffy", "c-law", "Luffy vs Law"));
        assertThat(sink.done.resources()).containsExactly(LUFFY, law);
    }

    @Test
    public void reusesLinksFromEarlierAnswersInAFollowUp() {
        when(store.recentTurns(USER)).thenReturn(List.of(
                AiTurn.user("Who is Luffy?"),
                AiTurn.assistant("[Luffy](onepiece://character/c-luffy) is the captain.", List.of(LUFFY))));
        client.script.add(answer("Yes, [Luffy](onepiece://character/c-luffy) again."));

        agent().run(USER, "Tell me more", AssistantLanguage.EN_US, sink);

        assertThat(roles(client.requests.get(0).messages())).containsExactly("system", "user", "assistant", "user");
        assertThat(sink.done.content()).isEqualTo("Yes, [Luffy](onepiece://character/c-luffy) again.");
    }

    @Test
    public void refusesToRunWithoutAKey() {
        when(settings.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> agent().run(USER, "Hi", AssistantLanguage.ZH_CN, sink))
                .isInstanceOfSatisfying(AssistantException.class,
                        e -> assertThat(e.code()).isEqualTo(AssistantException.NOT_CONFIGURED));
        assertThat(client.requests).isEmpty();
    }

    @Test
    public void fallsBackToAToolCapableModel() {
        when(settings.model()).thenReturn("deepseek-reasoner");
        client.script.add(answer("ok"));

        agent().run(USER, "Hi", AssistantLanguage.EN_US, sink);

        assertThat(client.requests.get(0).model()).isEqualTo(AssistantAgent.DEFAULT_MODEL);
    }

    @Test
    public void rejectsAnEmptyAnswer() {
        client.script.add(answer("   "));

        assertThatThrownBy(() -> agent().run(USER, "Hi", AssistantLanguage.EN_US, sink))
                .isInstanceOfSatisfying(AssistantException.class,
                        e -> assertThat(e.code()).isEqualTo(AssistantException.UPSTREAM_ERROR));
    }

    @Test
    public void confirmationBypassesTheModelEvenWithoutAnApiKey() {
        var actions = mock(ActionConfirmationService.class);
        var reply = new ActionConfirmationService.Reply("Completed", List.of(LUFFY));
        when(actions.handle(eq("confirm ABCDEF1234"), any())).thenReturn(Optional.of(reply));
        when(settings.isConfigured()).thenReturn(false);
        var agent = new AssistantAgent(settings, store, client, new ToolRegistry(List.of()), Runnable::run, actions);
        agent.run(USER, "confirm ABCDEF1234", AssistantLanguage.EN_US, sink);
        assertThat(client.requests).isEmpty();
        assertThat(sink.done.content()).isEqualTo("Completed");
        assertThat(saved).extracting(AiTurn::role).containsExactly("user", "assistant");
    }

    @Test
    public void proposalInstructionsArePersistedAndStreamedButNeverBecomeResourceChips() {
        var proposal = new ResourceLink(ActionConfirmationService.PROPOSAL, "ABCDEF1234", "Proposed change");
        var actions = mock(ActionConfirmationService.class);
        when(actions.handle(any(), any())).thenReturn(Optional.empty());
        when(actions.instructions(any(), any())).thenReturn("\nConfirm ABCDEF1234 to execute");
        var provider = (AssistantToolProvider) () -> List.of(tool("prepare_create_group", new ToolResult("{}", List.of(proposal))));
        var agent = new AssistantAgent(settings, store, client, new ToolRegistry(List.of(provider)), Runnable::run, actions);
        client.script.add(toolCall("call_1", "prepare_create_group", "{}"));
        client.script.add(answer("Prepared for review."));
        agent.run(USER, "Create a crew", AssistantLanguage.EN_US, sink);
        assertThat(sink.done.content()).contains("Confirm ABCDEF1234");
        assertThat(sink.streamed.toString()).contains("Confirm ABCDEF1234");
        assertThat(sink.done.resources()).isEmpty();
        verify(actions).publish(any(), any());
        verify(actions, never()).handle(eq("confirm ABCDEF1234"), any());
    }

    @Test
    public void actionableToolErrorsReachTheModelInsteadOfGenericLookupFailure() {
        var problem = new ToolProblem("Choose a character", new ToolResult("{\"error\":\"ambiguous_resource\"}", List.of(LUFFY)));
        var tool = new AssistantTool("get_character_profile", "test", schema(), (a,c) -> { throw problem; }, (a,l) -> "lookup");
        var agent = agent(tool);
        client.script.add(toolCall("call_1", "get_character_profile", "{}"));
        client.script.add(answer("Please choose a character."));
        agent.run(USER, "Which Luffy?", AssistantLanguage.EN_US, sink);
        assertThat(client.requests.get(1).messages().get(3).path("content").asText()).contains("ambiguous_resource");
    }

    @Test
    public void failedAnswersDiscardUnpublishedDrafts() {
        var actions = mock(ActionConfirmationService.class);
        when(actions.handle(any(), any())).thenReturn(Optional.empty());
        var provider = (AssistantToolProvider) () -> List.of(tool("prepare_create_group", new ToolResult("{}",
                List.of(new ResourceLink(ActionConfirmationService.PROPOSAL, "ABCDEF1234", "Draft")))));
        var agent = new AssistantAgent(settings, store, client, new ToolRegistry(List.of(provider)), Runnable::run, actions);
        client.script.add(toolCall("call_1", "prepare_create_group", "{}"));
        client.script.add(answer(""));
        assertThatThrownBy(() -> agent.run(USER, "Create a crew", AssistantLanguage.EN_US, sink))
                .isInstanceOf(AssistantException.class);
        verify(actions).discardUnpublished(any(), any());
        verify(actions, never()).publish(any(), any());
    }

    private AssistantAgent agent(AssistantTool... tools) {
        AssistantToolProvider provider = () -> List.of(tools);
        // A direct executor keeps the tool calls on the test thread.
        return new AssistantAgent(settings, store, client, new ToolRegistry(List.of(provider)), Runnable::run);
    }

    private AssistantTool tool(String name, ToolResult result) {
        return new AssistantTool(name, "test tool", schema(),
                (arguments, context) -> result, (arguments, language) -> "running " + name);
    }

    private ObjectNode schema() {
        return objectMapper.createObjectNode().put("type", "object");
    }

    private static Completion answer(String content) {
        return new Completion(content, List.of(), Usage.NONE);
    }

    private static Completion toolCall(String id, String name, String arguments) {
        return new Completion("", List.of(new ToolCall(id, name, arguments)), Usage.NONE);
    }

    private static List<String> roles(List<ObjectNode> messages) {
        return messages.stream().map(message -> message.get("role").asText()).toList();
    }

    /** Replays a fixed script; streams answers in two halves like the real client. */
    private static final class ScriptedClient implements ChatCompletionClient {
        private final Deque<Completion> script = new ArrayDeque<>();
        private final List<CompletionRequest> requests = new ArrayList<>();
        private Completion forever;
        private Completion whenToolsDisabled;

        @Override
        public Completion complete(CompletionRequest request, Consumer<String> onContentDelta) {
            requests.add(request);
            Completion next = request.tools() == null && whenToolsDisabled != null
                    ? whenToolsDisabled
                    : script.isEmpty() ? forever : script.poll();
            if (!next.hasToolCalls()) {
                String content = next.content();
                onContentDelta.accept(content.substring(0, content.length() / 2));
                onContentDelta.accept(content.substring(content.length() / 2));
            }
            return next;
        }
    }

    private static final class RecordingSink implements AssistantEventSink {
        private final List<String> events = new ArrayList<>();
        private final StringBuilder streamed = new StringBuilder();
        private DoneEvent done;

        @Override
        public void status(String tool, String label) {
            events.add("status:" + tool + ":" + label);
        }

        @Override
        public void delta(String text) {
            events.add("delta");
            streamed.append(text);
        }

        @Override
        public void done(DoneEvent done) {
            this.done = done;
        }

        @Override
        public void error(String code, String message) {
            events.add("error:" + code);
        }
    }
}
