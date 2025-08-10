package com.example.hotelwebhook.websocket;

import com.example.hotelwebhook.service.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
public class NotifyWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserId(session);
        String userType = getUserType(session);
        
        if (userId != null && userType != null) {
            sessionManager.registerSession(userId, userType, session);
            log.info("用户 {} (类型: {}) 建立WebSocket连接", userId, userType);
        } else {
            log.warn("WebSocket连接缺少必要参数: userId={}, userType={}", userId, userType);
            session.close();
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionManager.removeSession(session.getId());
        log.info("WebSocket连接关闭: {}", session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        
        // 处理心跳消息
        if ("ping".equalsIgnoreCase(payload)) {
            sessionManager.updateHeartbeat(session.getId());
            session.sendMessage(new TextMessage("pong"));
            return;
        }
        
        // 处理其他消息类型
        log.debug("收到WebSocket消息: {}", payload);
        
        // 这里可以添加其他消息处理逻辑
        // 比如：用户状态更新、输入状态等
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", exception.getMessage(), exception);
        sessionManager.removeSession(session.getId());
    }
    
    /**
     * 从URL参数中获取用户ID
     */
    private String getUserId(WebSocketSession session) {
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
    
    /**
     * 从URL参数中获取用户类型
     */
    private String getUserType(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "userType".equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        return "guest"; // 默认为客人
    }
} 