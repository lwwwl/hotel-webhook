package com.example.hotelwebhook.config;

import com.example.hotelwebhook.websocket.NotifyWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notifyWebSocketHandler(), "/ws/notify").setAllowedOrigins("*");
    }

    @Bean
    public NotifyWebSocketHandler notifyWebSocketHandler() {
        return new NotifyWebSocketHandler();
    }
} 