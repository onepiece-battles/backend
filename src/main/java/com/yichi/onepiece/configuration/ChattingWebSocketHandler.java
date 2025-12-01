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

@Component
public class ChattingWebSocketHandler extends AbstractWebSocketHandler {
    Logger logger = LoggerFactory.getLogger(ChattingWebSocketHandler.class);
    private final MessageService messageService;
    private final ChattingService chattingService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // userSocketMap 以后可能存在一个用户对应多个session的情况
    private final ConcurrentHashMap<UUID, WebSocketSession> userSocketMap = new ConcurrentHashMap<>();
    // 记录这个群组的哪些用户上线了
    private final ConcurrentHashMap<UUID, List<WebSocketSession>> groupMaps = new ConcurrentHashMap<>();

    public ChattingWebSocketHandler(MessageService messageService, ChattingService chattingService) {
        this.messageService = messageService;
        this.chattingService = chattingService;
    }


    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info(session.getAttributes().toString());
        logger.info("connection established");
        UUID userId =  UUID.fromString(session.getAttributes().get("userId").toString());
        List<Chat> chats = chattingService.list(new ListChatRequest().userId(userId));
        List<UUID> groupIds = chats.stream().map(Chat::getId).toList();
        for (UUID id: groupIds) {
            if(groupMaps.containsKey(id)) {
                groupMaps.get(id).add(session);
            } else {
                groupMaps.put(id, new ArrayList<>(List.of(session)));
            }
        }
        userSocketMap.putIfAbsent(userId, session);
        // update chat list
        for(Chat chat: chats) {
            String stringMessage = objectMapper.writeValueAsString(chat);
            session.sendMessage(new TextMessage(stringMessage));
        }
        // collect recent messages
        List<Message> messageFullViewList = groupIds
                .stream().map(messageService::getRecentMessage)
                .toList().stream().flatMap(Collection::stream)
                .map(ChatConverter.INSTANCE::toMessage)
                .toList();
        for(Message message: messageFullViewList) {
            String stringMessage = objectMapper.writeValueAsString(message);
            logger.info(stringMessage);
            session.sendMessage(new TextMessage(stringMessage));
        }
    }

    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("connection closed");
        UUID userId = UUID.fromString(session.getAttributes().get("userId").toString()) ;
        userSocketMap.remove(userId);
        List<UUID> groupIds = chattingService.listUserConversationByUserId(userId).stream().map(UserConversationEntity::getConversationId).toList();
        for(UUID id: groupIds) {
            groupMaps.get(id).remove(session);
        }
        session.close(status);
    }


    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode jsonNode = objectMapper.readTree(payload);
        String messageType = jsonNode.has("type") ? jsonNode.get("type").asText() : null;
        logger.info("message type: %s", messageType);
        if (MessageType.SEND_MESSAGE.toString().equals(messageType)) {
            logger.info("send message");
            SendMessageRequest request = objectMapper.readValue(message.getPayload(), SendMessageRequest.class);
            var response = messageService.sendMessage(request);
            if (response.getResult()) {
                sendMessages(request.getConversationId(), new TextMessage(objectMapper.writeValueAsString(response.getMessage())));
            }
        } else if (MessageType.JOIN_CHAT.toString().equals(messageType)) {
            logger.info("join chat");
            JoinChatRequest request = objectMapper.readValue(message.getPayload(), JoinChatRequest.class);
            var chat = chattingService.joinChat(request);
            if (chat!=null) {
                sendMessages(chat.getId(), message);
            }
        } else if (MessageType.CREATE_CHAT.toString().equals(messageType)) {
            logger.info("create chat");
            CreateChatRequest request = objectMapper.readValue(message.getPayload(), CreateChatRequest.class);
            var chat = chattingService.create(request);
            if (chat!=null) {
                sendMessages(chat.getId(), message);
            }
        }
    }

    void sendMessages(UUID conversationId, TextMessage message) throws IOException {
        List<WebSocketSession> sessions = groupMaps.get(conversationId);
        for(WebSocketSession socketSession: sessions) {
            logger.info(message.getPayload());
            socketSession.sendMessage(message);
        }
    }
}
