package com.yichi.onepiece.configuration;

import com.yichi.onepiece.chatting.service.ChattingService;
import com.yichi.onepiece.chatting.service.MessageService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.HashSet;

@Component
public class ChattingWebSocketHandler extends AbstractWebSocketHandler {

    private final MessageService messageService;

    public ChattingWebSocketHandler(MessageService messageService) {
        this.messageService = messageService;
    }


    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

    }

}
