package com.yichi.onepiece.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.chatting.model.ChatConverter;
import com.yichi.onepiece.chatting.service.ChattingService;
import com.yichi.onepiece.chatting.service.MessageService;
import com.yichi.onepiece.model.Message;
import com.yichi.onepiece.model.SendMessageRequest;
import com.yichi.onepiece.user.UserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
        UUID userId =  UUID.fromString(session.getAttributes().get("userId").toString());
        List<UUID> groupIds = chattingService.listUserConversationByUserId(userId).stream().map(e->e.getConversationId()).toList();
        for (UUID id: groupIds) {
            if(groupMaps.containsKey(id)) {
                groupMaps.get(id).add(session);
            } else {
                groupMaps.put(id, new ArrayList<>(Arrays.asList(session)));
            }
        }
        var conversationIds = chattingService.listUserConversationByUserId(userId).stream().map(e->e.getConversationId()).toList();
        userSocketMap.put(userId, session);
        // collect recent messages
        List<Message> messageFullViewList = conversationIds
                .stream().map(e-> messageService.getRecentMessage(e))
                .toList().stream().flatMap(list->list.stream())
                .map(e-> ChatConverter.INSTANCE.toMessage(e))
                .collect(Collectors.toList());
        for(Message message: messageFullViewList) {
            String stringMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(stringMessage));
        }
    }

    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        logger.info("connection closed");
        UUID userId = UUID.fromString(session.getAttributes().get("userId").toString()) ;
        userSocketMap.remove(userId);
        List<UUID> groupIds = chattingService.listUserConversationByUserId(userId).stream().map(e->e.getConversationId()).toList();
        for(UUID id: groupIds) {
            groupMaps.get(id).remove(session);
        }
        session.close(status);
    }


    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SendMessageRequest request = objectMapper.readValue(message.getPayload(), SendMessageRequest.class);
        var response = messageService.sendMessage(request);
        if (response.getResult()) {
            List<WebSocketSession> sessions = groupMaps.get(request.getChatId());
            for(WebSocketSession socketSession: sessions) {
                socketSession.sendMessage(message);
            }
        }
    }
}
