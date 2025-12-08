package com.yichi.onepiece.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

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

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // Size in bytes (e.g., 5mb)
        container.setMaxTextMessageBufferSize(512 * 1024 * 10);
        container.setMaxBinaryMessageBufferSize(512 * 1024 * 10);
        return container;
    }
}
