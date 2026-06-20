package com.yichi.onepiece.configuration;

import java.util.Arrays;
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
    private final AuthHandshakeInterceptor interceptor;

    private final ChattingWebSocketHandler chattingWebSocketHandler;

    public WebSocketConfig(ChattingWebSocketHandler chattingWebSocketHandler, AuthHandshakeInterceptor interceptor) {
        this.chattingWebSocketHandler = chattingWebSocketHandler;
        this.interceptor = interceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chattingWebSocketHandler, "/chat").addInterceptors(interceptor).setAllowedOrigins(getAllowedOrigins());
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // Size in bytes (e.g., 5mb)
        container.setMaxTextMessageBufferSize(512 * 1024 * 10);
        container.setMaxBinaryMessageBufferSize(512 * 1024 * 10);
        return container;
    }

    private String[] getAllowedOrigins() {
        String origins = System.getenv("ONEPIECE_ALLOWED_ORIGINS");
        if (origins == null || origins.isBlank()) {
            origins = "*";
        }
        return Arrays.stream(origins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new);
    }
}
