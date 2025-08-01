package com.example.hotelwebhook.websocket;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class NotifyWebSocketHandler extends TextWebSocketHandler {
    // 用户ID到WebSocketSession的映射
    private static final Map<String, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();

    public static WebSocketSession getSession(String userId) {
        return userSessionMap.get(userId);
    }

    public static void sendNotify(String userId) {
        WebSocketSession session = userSessionMap.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage("notify"));
            } catch (Exception e) {
                // 日志省略
            }
        }
    }

    public static Map<String, WebSocketSession> getUserSessionMap() {
        return userSessionMap;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 假设前端连接时带userId参数
        String userId = getUserId(session);
        if (userId != null) {
            userSessionMap.put(userId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserId(session);
        if (userId != null) {
            userSessionMap.remove(userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 心跳包或其他消息
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    private String getUserId(WebSocketSession session) {
        // 从URL参数获取userId
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "userId".equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        return null;
    }
} 