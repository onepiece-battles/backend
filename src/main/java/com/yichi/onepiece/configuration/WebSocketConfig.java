package com.yichi.onepiece.configuration;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private static final int MAX_MESSAGE_SIZE_BYTES = 9 * 1024 * 1024;
    private final AuthHandshakeInterceptor interceptor;

    private final ChattingWebSocketHandler chattingWebSocketHandler;

    public WebSocketConfig(ChattingWebSocketHandler chattingWebSocketHandler, AuthHandshakeInterceptor interceptor) {
        this.chattingWebSocketHandler = chattingWebSocketHandler;
        this.interceptor = interceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chattingWebSocketHandler, "/chat")
                .addInterceptors(interceptor)
                .setAllowedOriginPatterns(getAllowedOrigins());
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(MAX_MESSAGE_SIZE_BYTES);
        container.setMaxBinaryMessageBufferSize(MAX_MESSAGE_SIZE_BYTES);
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
