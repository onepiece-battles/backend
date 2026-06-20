package com.yichi.onepiece.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.chatting.model.ChatConverter;
import com.yichi.onepiece.chatting.model.UserConversationEntity;
import com.yichi.onepiece.chatting.service.ChattingService;
import com.yichi.onepiece.chatting.service.MessageService;
import com.yichi.onepiece.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ChattingWebSocketHandler extends AbstractWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChattingWebSocketHandler.class);

    private final MessageService messageService;
    private final ChattingService chattingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1:1 Mapping: Active User -> Latest Session
    private final ConcurrentHashMap<UUID, WebSocketSession> userSocketMap = new ConcurrentHashMap<>();

    // Group Mapping: Group ID -> Thread-Safe Set of Sessions
    // We use CopyOnWriteArraySet because iteration happens often (sending msgs), but modification (join/leave) is less frequent.
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> groupMaps = new ConcurrentHashMap<>();

    public ChattingWebSocketHandler(MessageService messageService, ChattingService chattingService) {
        this.messageService = messageService;
        this.chattingService = chattingService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object userIdObj = session.getAttributes().get("userId");
        if (userIdObj == null) {
            logger.error("No userId found in session attributes, closing session");
            session.close();
            return;
        }

        UUID userId = UUID.fromString(userIdObj.toString());
        logger.info("User id: {} established connection", userId);

        // FIX: Always put the latest session.
        // Note: If you want to support multiple devices per user, this map needs to be UUID -> Set<Session>
        userSocketMap.put(userId, session);
        logger.info("user socket map: {}", userSocketMap);

        // Load user's chats
        List<Chat> chats = chattingService.list(new ListChatRequest().userId(userId));
        List<UUID> groupIds = chats.stream().map(Chat::getId).toList();

        // FIX: Atomic computation to prevent race conditions
        for (UUID groupId : groupIds) {
            groupMaps.computeIfAbsent(groupId, k -> new CopyOnWriteArraySet<>()).add(session);
        }

        // Update chat list
        for (Chat chat : chats) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(chat)));
        }

        // Collect recent messages
        // Optimization: Use parallel stream if message volume is high, but keep sequential for ordering if needed
        List<Message> messageFullViewList = groupIds.stream()
                .map(messageService::getRecentMessage)
                .flatMap(Collection::stream)
                .map(ChatConverter.INSTANCE::toMessage)
                .toList(); // Java 17 toList()

        for (Message message : messageFullViewList) {
            String stringMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(stringMessage));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("Connection closed");
        Object userIdObj = session.getAttributes().get("userId");
        if (userIdObj != null) {
            UUID userId = UUID.fromString(userIdObj.toString());

            // FIX: Only remove if the map currently holds THIS specific session.
            // This prevents removing the user's *new* tab if they opened a second one before closing the first.
            logger.info("before removing: {}", userSocketMap);
            logger.info("remove user: {} from user socket map", userId);
            userSocketMap.remove(userId, session);
            logger.info("after removing: {}", userSocketMap);
            // Clean up group memberships
            List<UserConversationEntity> conversations = chattingService.listUserConversationByUserId(userId);
            if (conversations != null) {
                for (UserConversationEntity conv : conversations) {
                    Set<WebSocketSession> sessions = groupMaps.get(conv.getConversationId());
                    if (sessions != null) {
                        sessions.remove(session);
                        // Optional: remove the group entry if empty to save memory
                        if (sessions.isEmpty()) {
                            groupMaps.remove(conv.getConversationId());
                        }
                    }
                }
            }
        }
        // No need to call session.close(status), the framework does it.
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        // --- FIX START: Handle Heartbeat ---
        // Check if the message is just a simple "ping" string
        if ("ping".equalsIgnoreCase(payload)) {
            // Optional: Reply with "pong" so the frontend knows the server is alive
            // session.sendMessage(new TextMessage("pong"));
            logger.debug("Received heartbeat from user");
            return; // Stop processing here, do not try to parse JSON
        }
        JsonNode jsonNode = objectMapper.readTree(payload);
        String messageType = jsonNode.has("type") ? jsonNode.get("type").asText() : "UNKNOWN";
        UUID userId = UUID.fromString(session.getAttributes().get("userId").toString());

        if (MessageType.SEND_MESSAGE.toString().equals(messageType)) {
            SendMessageRequest request = objectMapper.readValue(payload, SendMessageRequest.class);
            var response = messageService.sendMessage(request, userId);
            if (response.getResult()) {
                sendMessagesToGroup(request.getConversationId(), new TextMessage(objectMapper.writeValueAsString(response.getMessage())));
            }
        } else if (MessageType.JOIN_CHAT.toString().equals(messageType)) {
            JoinChatRequest request = objectMapper.readValue(payload, JoinChatRequest.class);
            var chat = chattingService.joinChat(request, userId);
            if (chat != null) {
                // Logic for Join might also need to update groupMaps if the user wasn't in it before
                registerUsersToGroup(new ArrayList<>(request.getIds()), chat.getId());
                sendMessagesToUsers(new ArrayList<>(request.getIds()), new TextMessage(objectMapper.writeValueAsString(chat)));
            }
        } else if (MessageType.CREATE_CHAT.toString().equals(messageType)) {
            CreateChatRequest request = objectMapper.readValue(payload, CreateChatRequest.class);
            var chat = chattingService.create(request, userId);
            if (chat != null) {
                // 1. Send the New Chat Object to frontend (so they can render the chat box)
                sendMessagesToUsers(request.getIds(), new TextMessage(objectMapper.writeValueAsString(chat)));

                // --- FIX STARTS HERE ---
                // 2. We must register the current active sessions to this NEW group ID in memory.
                // Otherwise, 'sendMessagesToGroup' won't know who to send to.
                registerUsersToGroup(request.getIds(), chat.getId());
                // --- FIX ENDS HERE ---
                String firstMessageContent;
                if (chat.getGroup()) {
                    firstMessageContent = "我创建了一个群组，大家一起聊天吧";
                } else {
                   firstMessageContent="我通过了你的朋友验证请求，现在我们可以开始聊天了";
                }
                logger.info(firstMessageContent);
                    var response = messageService.sendMessage(
                            new SendMessageRequest()
                                    .conversationId(chat.getId())
                                    .type(MessageType.SEND_MESSAGE)
                                    .content(firstMessageContent)
                                    .userId(userId),
                            userId);
                if (response.getResult()) {
                    logger.info("send message to new group");
                    // 3. Now this will work because the map is updated
                    sendMessagesToGroup(chat.getId(), new TextMessage(objectMapper.writeValueAsString(response.getMessage())));
                }
            }
        }
    }

    /**
     * Helper method to bind active user sessions to a specific group ID.
     */
    private void registerUsersToGroup(List<UUID> userIds, UUID groupId) {
        for (UUID userId : userIds) {
            WebSocketSession userSession = userSocketMap.get(userId);
            // Only add if the user is currently online (has a session)
            if (userSession != null && userSession.isOpen()) {
                groupMaps.computeIfAbsent(groupId, k -> new CopyOnWriteArraySet<>()).add(userSession);
            }
        }
    }


    private void sendMessagesToGroup(UUID conversationId, TextMessage message) {
        Set<WebSocketSession> sessions = groupMaps.get(conversationId);
        // FIX: Null check and isOpen check
        if (sessions != null) {
            for (WebSocketSession socketSession : sessions) {
                sendMessageSafely(socketSession, message);
            }
        }
    }

    private void sendMessagesToUsers(List<UUID> userIds, TextMessage message) {
        logger.info("users in socket: {}", userIds.stream()
                .map(userSocketMap::get)
                .filter(Objects::nonNull).toList());

        // FIX: Handle nulls if user is offline
        userIds.stream()
                .map(userSocketMap::get)
                .filter(Objects::nonNull) // Filter out offline users
                .forEach(session -> sendMessageSafely(session, message));
    }

    private void sendMessageSafely(WebSocketSession session, TextMessage message) {
        try {
            if (session.isOpen()) {
                logger.info("sending message: {}", message);
                session.sendMessage(message);
            }
        } catch (IOException e) {
            logger.error("Error sending message to session {}", session.getId(), e);
        }
    }

    public void disconnectUser(UUID userId) {
        WebSocketSession session = userSocketMap.get(userId);
        if (session != null && session.isOpen()) {
            try {
                logger.info("Force closing session for user: {}", userId);
                // Close with a normal status code
                session.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                logger.error("Error closing session for user {}", userId, e);
            }
            // Note: session.close() will trigger afterConnectionClosed(),
            // which handles the map cleanup logic you already wrote.
        }
    }
}
