package com.yichi.onepiece.assistant.dto;

import java.util.List;

/**
 * Request/response payloads for the AI assistant endpoints. Plain Jackson
 * records (camelCase JSON), kept out of the OpenAPI spec on purpose so the
 * proxy can stay schema-agnostic and evolve without regenerating clients.
 */
public final class AssistantDto {
    private AssistantDto() {}

    /** A resource the answer links to; rendered as an inline tappable link. */
    public record ResourceLink(String type, String id, String name) {}

    /** Proxy request: the full DeepSeek chat payload, serialized as JSON. */
    public record AiChatRequest(String requestBody) {}

    /** Proxy response: DeepSeek's raw JSON plus the upstream HTTP status. */
    public record AiChatResponse(int status, String responseBody, String error) {}

    /** NIKA-only: write the provider settings. A blank apiKey leaves it unchanged. */
    public record AiSettingRequest(String apiKey, String baseUrl, String model) {}

    /** NIKA-only read: whether a key is set and the current endpoint/model. Never the key. */
    public record AiSettingResponse(boolean configured, String baseUrl, String model) {}

    public record SaveAiMessageRequest(String role, String content, List<ResourceLink> resources) {}

    public record SaveAiMessageResponse(String id) {}

    public record AiMessageView(String id, String role, String content, List<ResourceLink> resources) {}

    public record ListAiMessagesResponse(List<AiMessageView> messages) {}

    public record SimpleResult(boolean result) {}

    // ----- server-side agent (/ask, answered as a text/event-stream) -----

    /** A question for the agent. {@code language} is the client's UI language (zh_CN / en_US), optional. */
    public record AskRequest(String message, String language) {}

    /** {@code event: status} — a tool is running. Drop any answer text streamed so far. */
    public record StatusEvent(String tool, String label) {}

    /** {@code event: delta} — the next fragment of the answer. */
    public record DeltaEvent(String text) {}

    /** A battle the assistant prepared for the user to confirm and create. */
    public record BattleProposal(String starterId, String receiverId, String label) {}

    /** {@code event: done} — the final answer with links repaired; replaces the streamed text. */
    public record DoneEvent(
            String messageId, String content, List<ResourceLink> resources, BattleProposal battleProposal) {}

    /** {@code event: error} — the answer failed; {@code code} is stable, {@code message} is for display. */
    public record ErrorEvent(String code, String message) {}
}
