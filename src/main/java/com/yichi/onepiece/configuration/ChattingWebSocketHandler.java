package com.yichi.onepiece.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.chatting.model.ChatConverter;
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
    private static final int MAX_MESSAGE_SIZE_BYTES = 9 * 1024 * 1024;
    private static final String SESSION_GROUP_IDS = "chatGroupIds";

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
        WebSocketSession previousSession = userSocketMap.put(userId, session);
        if (previousSession != null && previousSession != session && previousSession.isOpen()) {
            try {
                previousSession.close(CloseStatus.NORMAL.withReason("Replaced by a newer connection"));
            } catch (IOException e) {
                logger.debug("Failed to close replaced WebSocket session {}", previousSession.getId(), e);
            }
        }

        Set<UUID> sessionGroupIds = ConcurrentHashMap.newKeySet();
        session.getAttributes().put(SESSION_GROUP_IDS, sessionGroupIds);

        try {
            initializeSession(session, userId, sessionGroupIds);
        } catch (RuntimeException e) {
            // A history/query problem must not tear down an authenticated live socket.
            logger.error(
                    "WebSocket connected for user {}, but initial chat synchronization failed",
                    userId,
                    e);
        }
    }

    private void initializeSession(
            WebSocketSession session, UUID userId, Set<UUID> sessionGroupIds) {
        List<Chat> chats = chattingService.list(userId);
        List<UUID> groupIds = chats.stream().map(Chat::getId).toList();
        sessionGroupIds.addAll(groupIds);

        for (UUID groupId : groupIds) {
            groupMaps.computeIfAbsent(groupId, ignored -> new CopyOnWriteArraySet<>()).add(session);
        }

        for (Chat chat : chats) {
            try {
                sendMessageSafely(session, new TextMessage(objectMapper.writeValueAsString(chat)));
            } catch (RuntimeException | IOException e) {
                logger.warn("Failed to synchronize chat {} to session {}", chat.getId(), session.getId(), e);
            }
        }

        for (UUID groupId : groupIds) {
            try {
                for (var message : messageService.getRecentMessage(groupId)) {
                    String stringMessage =
                            objectMapper.writeValueAsString(ChatConverter.INSTANCE.toMessage(message));
                    sendMessageSafely(session, new TextMessage(stringMessage));
                }
            } catch (RuntimeException | IOException e) {
                logger.warn(
                        "Failed to synchronize recent messages for chat {} to session {}",
                        groupId,
                        session.getId(),
                        e);
            }
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
            userSocketMap.remove(userId, session);
            Object groupIds = session.getAttributes().get(SESSION_GROUP_IDS);
            if (groupIds instanceof Set<?> groups) {
                for (Object groupId : groups) {
                    if (groupId instanceof UUID conversationId) {
                        Set<WebSocketSession> sessions = groupMaps.get(conversationId);
                        if (sessions != null) {
                            sessions.remove(session);
                            if (sessions.isEmpty()) {
                                groupMaps.remove(conversationId, sessions);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (message.getPayloadLength() > MAX_MESSAGE_SIZE_BYTES) {
            logger.warn(
                    "Closing session {} after oversized chat payload: {} bytes",
                    session.getId(),
                    message.getPayloadLength());
            session.close(CloseStatus.TOO_BIG_TO_PROCESS);
            return;
        }
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload)) {
            logger.debug("Received heartbeat from user");
            return;
        }

        String messageType;
        try {
            messageType = readMessageType(payload);
        } catch (IOException e) {
            logger.warn("Ignoring malformed WebSocket message from session {}", session.getId());
            return;
        }
        if ("ping".equalsIgnoreCase(messageType)) {
            logger.debug("Received heartbeat from user");
            return;
        }
        if (messageType == null) {
            logger.warn("Ignoring WebSocket message without a type from session {}", session.getId());
            return;
        }

        UUID userId = UUID.fromString(session.getAttributes().get("userId").toString());

        if (MessageType.SEND_MESSAGE.getValue().equals(messageType)) {
            SendMessageRequest request = objectMapper.readValue(payload, SendMessageRequest.class);
            var response = messageService.sendMessage(request, userId);
            if (response.getResult()) {
                registerSessionToGroup(session, request.getConversationId());
                sendMessagesToGroup(request.getConversationId(), new TextMessage(objectMapper.writeValueAsString(response.getMessage())));
            }
        } else if (MessageType.JOIN_CHAT.getValue().equals(messageType)) {
            JoinChatRequest request = objectMapper.readValue(payload, JoinChatRequest.class);
            var chat = chattingService.joinChat(request, userId);
            if (chat != null) {
                // Logic for Join might also need to update groupMaps if the user wasn't in it before
                registerUsersToGroup(new ArrayList<>(request.getIds()), chat.getId());
                sendMessagesToUsers(new ArrayList<>(request.getIds()), new TextMessage(objectMapper.writeValueAsString(chat)));
            }
        } else if (MessageType.CREATE_CHAT.getValue().equals(messageType)) {
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
        } else {
            logger.warn(
                    "Ignoring unsupported WebSocket message type '{}' from session {}",
                    messageType,
                    session.getId());
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
                getSessionGroupIds(userSession).add(groupId);
            }
        }
    }

    private void registerSessionToGroup(WebSocketSession session, UUID groupId) {
        groupMaps.computeIfAbsent(groupId, ignored -> new CopyOnWriteArraySet<>()).add(session);
        getSessionGroupIds(session).add(groupId);
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
        // FIX: Handle nulls if user is offline
        userIds.stream()
                .map(userSocketMap::get)
                .filter(Objects::nonNull) // Filter out offline users
                .forEach(session -> sendMessageSafely(session, message));
    }

    private String readMessageType(String payload) throws IOException {
        try (JsonParser parser = objectMapper.createParser(payload)) {
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME
                        && "type".equals(parser.currentName())
                        && parser.nextToken() == JsonToken.VALUE_STRING) {
                    return parser.getValueAsString();
                }
            }
        }
        return null;
    }

    private void sendMessageSafely(WebSocketSession session, TextMessage message) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            logger.error("Error sending message to session {}", session.getId(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<UUID> getSessionGroupIds(WebSocketSession session) {
        return (Set<UUID>) session.getAttributes().computeIfAbsent(
                SESSION_GROUP_IDS, ignored -> ConcurrentHashMap.newKeySet());
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
