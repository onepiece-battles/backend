package com.yichi.onepiece.assistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.dto.AssistantDto.AiChatResponse;
import com.yichi.onepiece.assistant.dto.AssistantDto.AiMessageView;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.assistant.model.AiConversationDao;
import com.yichi.onepiece.assistant.model.AiConversationEntity;
import com.yichi.onepiece.assistant.model.AiConversationEntityBuilder;
import com.yichi.onepiece.assistant.model.AiMessageDao;
import com.yichi.onepiece.assistant.model.AiMessageEntity;
import com.yichi.onepiece.assistant.model.AiMessageEntityBuilder;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Proxies chat completions to DeepSeek using the server-held key (so the key
 * never reaches any client) and persists each user's running AI thread.
 */
@Service
public class AiAssistantService {
    private static final Logger logger = LoggerFactory.getLogger(AiAssistantService.class);
    private static final int MAX_HISTORY = 200;

    private final AiSettingService settingService;
    private final AiConversationDao conversationDao;
    private final AiMessageDao messageDao;
    private final ActionRecordService actionRecordService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public AiAssistantService(AiSettingService settingService, AiConversationDao conversationDao,
            AiMessageDao messageDao, ActionRecordService actionRecordService) {
        this.settingService = settingService;
        this.conversationDao = conversationDao;
        this.messageDao = messageDao;
        this.actionRecordService = actionRecordService;
    }

    // ----- proxy -----

    public AiChatResponse chat(String requestBody) {
        if (!settingService.isConfigured()) {
            return new AiChatResponse(0, null, "not_configured");
        }
        String body = injectModel(requestBody, settingService.model());
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(settingService.baseUrl()))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + settingService.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new AiChatResponse(response.statusCode(), response.body(), null);
        } catch (Exception e) {
            logger.error("DeepSeek proxy call failed", e);
            return new AiChatResponse(0, null, "upstream_error");
        }
    }

    /** Override the payload's model with the admin-configured one when set. */
    private String injectModel(String requestBody, String model) {
        if (model == null || model.isBlank()) {
            return requestBody;
        }
        try {
            var node = objectMapper.readTree(requestBody);
            if (node instanceof ObjectNode objectNode) {
                objectNode.put("model", model);
                return objectMapper.writeValueAsString(objectNode);
            }
        } catch (Exception e) {
            logger.warn("could not inject model into AI request body, forwarding as-is", e);
        }
        return requestBody;
    }

    // ----- persistence -----

    /** The visible thread: questions and final answers, without the hidden tool rounds. */
    public List<AiMessageView> listMessages(UUID userId) {
        AiConversationEntity conversation = getOrCreateConversation(userId);
        return messageDao.listByConversation(conversation.getId(), MAX_HISTORY).stream()
                .filter(AiAssistantService::isVisible)
                .map(this::toView)
                .toList();
    }

    public String saveMessage(UUID userId, String role, String content, List<ResourceLink> resources) {
        AiConversationEntity conversation = getOrCreateConversation(userId);
        ActionRecordEntity record = actionRecordService.createWithUserId(userId);
        AiMessageEntity entity = AiMessageEntityBuilder.newBuilder()
                .setConversationId(conversation.getId())
                .setUserId(userId)
                .setRole(role)
                .setContent(content)
                .setResources(serializeResources(resources))
                .setCreationRecordId(record.getId())
                .setModificationRecordId(record.getId())
                .build();
        messageDao.insert(entity);
        return entity.getId().toString();
    }

    /** Stores the messages of one agent turn, in order, and returns their ids. */
    public List<String> saveTurns(UUID userId, List<AiTurn> turns) {
        AiConversationEntity conversation = getOrCreateConversation(userId);
        List<String> ids = new ArrayList<>();
        for (AiTurn turn : turns) {
            // One action record per message: creation_record_id is what orders the thread.
            ActionRecordEntity record = actionRecordService.createWithUserId(userId);
            AiMessageEntity entity = AiMessageEntityBuilder.newBuilder()
                    .setConversationId(conversation.getId())
                    .setUserId(userId)
                    .setRole(turn.role())
                    .setContent(turn.content())
                    .setToolCalls(turn.toolCalls())
                    .setToolCallId(turn.toolCallId())
                    .setResources(serializeResources(turn.resources()))
                    .setCreationRecordId(record.getId())
                    .setModificationRecordId(record.getId())
                    .build();
            messageDao.insert(entity);
            ids.add(entity.getId().toString());
        }
        return ids;
    }

    /** The newest stored messages, oldest first, including the hidden tool rounds. */
    public List<AiTurn> recentTurns(UUID userId) {
        return conversationDao.findByUserId(userId)
                .map(conversation -> messageDao.listByConversation(conversation.getId(), MAX_HISTORY).stream()
                        .map(entity -> new AiTurn(entity.getRole(), entity.getContent(), entity.getToolCalls(),
                                entity.getToolCallId(), deserializeResources(entity.getResources())))
                        .toList())
                .orElse(List.of());
    }

    public void clear(UUID userId) {
        conversationDao.findByUserId(userId)
                .ifPresent(conversation -> messageDao.deleteByConversation(conversation.getId()));
    }

    private AiConversationEntity getOrCreateConversation(UUID userId) {
        return conversationDao.findByUserId(userId).orElseGet(() -> {
            ActionRecordEntity record = actionRecordService.createWithUserId(userId);
            AiConversationEntity entity = AiConversationEntityBuilder.newBuilder()
                    .setUserId(userId)
                    .setCreationRecordId(record.getId())
                    .setModificationRecordId(record.getId())
                    .build();
            conversationDao.insert(entity);
            return entity;
        });
    }

    private static boolean isVisible(AiMessageEntity entity) {
        return new AiTurn(entity.getRole(), null, entity.getToolCalls(), null, null).visible();
    }

    private AiMessageView toView(AiMessageEntity entity) {
        return new AiMessageView(
                entity.getId().toString(),
                entity.getRole(),
                entity.getContent(),
                deserializeResources(entity.getResources()));
    }

    private String serializeResources(List<ResourceLink> resources) {
        if (resources == null || resources.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(resources);
        } catch (Exception e) {
            logger.warn("failed to serialize AI message resources", e);
            return null;
        }
    }

    private List<ResourceLink> deserializeResources(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ResourceLink>>() {});
        } catch (Exception e) {
            logger.warn("failed to parse AI message resources", e);
            return new ArrayList<>();
        }
    }
}
