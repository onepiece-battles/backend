package com.yichi.onepiece.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChattingWebSocketHandler chattingWebSocketHandler;

    public WebSocketConfig(ChattingWebSocketHandler chattingWebSocketHandler) {
        this.chattingWebSocketHandler = chattingWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chattingWebSocketHandler, "/chat").setAllowedOrigins("*");
    }
}
