package com.yichi.onepiece.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final AuthHandshakeInterceptor interceptor = new AuthHandshakeInterceptor();

    private final ChattingWebSocketHandler chattingWebSocketHandler;

    public WebSocketConfig(ChattingWebSocketHandler chattingWebSocketHandler) {
        this.chattingWebSocketHandler = chattingWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chattingWebSocketHandler, "/chat").addInterceptors(interceptor).setAllowedOrigins("*");
    }
}
