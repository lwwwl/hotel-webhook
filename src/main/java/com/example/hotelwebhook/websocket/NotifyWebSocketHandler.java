package com.example.hotelwebhook.websocket;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.example.hotelwebhook.service.WebSocketSessionManager;
import com.example.hotelwebhook.utils.ConnectionUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NotifyWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    @Autowired
    private ConnectionUtil connectionUtil;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserId(session);
        String contactId = getContactId(session);
        String connectionId = getConnectionId(session);
        
        // 根据参数判断用户类型
        String actualUserId = null;
        String userType = null;
        
        if (userId != null && !userId.isEmpty()) {
            // 客服端连接
            actualUserId = userId;
            userType = "agent";
        } else if (contactId != null && !contactId.isEmpty()) {
            // 客人端连接
            actualUserId = contactId;
            userType = "guest";
        }
        
        // 验证connectionId
        if (connectionId != null && !connectionId.isEmpty()) {
            // 首先验证connectionId格式是否有效
            if (!connectionUtil.isValidConnectionId(connectionId)) {
                log.warn("连接标识格式无效: connectionId={}", connectionId);
                session.close();
                return;
            }
            
            String expectedUserId = connectionUtil.extractUserId(connectionId);
            String expectedUserType = connectionUtil.extractUserType(connectionId);
            
            // 验证userId和userType是否匹配
            if (expectedUserId == null || expectedUserType == null) {
                log.warn("连接标识解析失败: connectionId={}", connectionId);
                session.close();
                return;
            }
            
            if (actualUserId == null || !actualUserId.equals(expectedUserId)) {
                log.warn("连接标识验证失败: userId不匹配, 实际={}, 期望={}, connectionId={}", 
                        actualUserId, expectedUserId, connectionId);
                session.close();
                return;
            }
            
            if (!userType.equals(expectedUserType)) {
                log.warn("连接标识验证失败: userType不匹配, 实际={}, 期望={}, connectionId={}", 
                        userType, expectedUserType, connectionId);
                session.close();
                return;
            }

            // 可选：验证连接时间是否在合理范围内（24小时内）
            Long connectionTimestamp = connectionUtil.extractTimestamp(connectionId);
            if (connectionTimestamp != null) {
                long now = System.currentTimeMillis();
                long expirationTime = connectionTimestamp + (24 * 60 * 60 * 1000); // 24小时
                if (now > expirationTime) {
                    log.warn("连接标识已过期: connectionTime={}, now={}, connectionId={}", 
                            connectionTimestamp, now, connectionId);
                    session.close();
                    return;
                }
            }
        } else {
            log.warn("WebSocket连接缺少connectionId参数");
            session.close();
            return;
        }

        sessionManager.registerSession(actualUserId, userType, session);
        log.info("用户 {} (类型: {}) 建立WebSocket连接, 连接ID: {}",
                actualUserId, userType, connectionId);
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
     * 从URL参数中获取用户ID（客服端）
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
     * 从URL参数中获取客人ID（客人端）
     */
    private String getContactId(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "contactId".equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        return null;
    }
    
    /**
     * 从URL参数中获取连接标识
     */
    private String getConnectionId(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "connectionId".equals(kv[0])) {
                    return kv[1];
                }
            }
        }
        return null;
    }
} 