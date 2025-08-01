package com.example.hotelwebhook.service;

import com.example.hotelwebhook.websocket.NotifyWebSocketHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;

import java.util.Map;

@Component
public class WebSocketHeartbeatTask {
    // 每30秒发送一次心跳
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        for (Map.Entry<String, WebSocketSession> entry : NotifyWebSocketHandler.getUserSessionMap().entrySet()) {
            WebSocketSession session = entry.getValue();
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage("ping"));
                } catch (Exception e) {
                    // 日志省略
                }
            }
        }
    }
} 