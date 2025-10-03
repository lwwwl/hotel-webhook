package com.example.hotelwebhook.service;

import com.example.hotelwebhook.model.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WebSocketSessionManager {
    
    // 客人ID到会话的映射 (支持多端登录)
    private final Map<String, Map<String, UserSession>> contactSessions = new ConcurrentHashMap<>();
    
    // 客服ID到会话的映射 (支持多端登录)
    private final Map<String, Map<String, UserSession>> agentSessions = new ConcurrentHashMap<>();
    
    // 会话ID到用户会话的映射
    private final Map<String, UserSession> sessionMap = new ConcurrentHashMap<>();
    
    /**
     * 注册用户会话
     */
    public void registerSession(String userId, String userType, WebSocketSession webSocketSession) {
        String sessionId = webSocketSession.getId();
        
        UserSession userSession = UserSession.builder()
                .userId(userId)
                .userType(userType)
                .sessionId(sessionId)
                .webSocketSession(webSocketSession)
                .connectedAt(LocalDateTime.now())
                .lastHeartbeat(LocalDateTime.now())
                .userAgent(webSocketSession.getHandshakeHeaders().getFirst("User-Agent"))
                .ipAddress(getClientIpAddress(webSocketSession))
                .build();
        
        // 根据用户类型添加到对应的映射
        if ("guest".equals(userType)) {
            contactSessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(sessionId, userSession);
        } else if ("agent".equals(userType)) {
            agentSessions.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(sessionId, userSession);
        }
        
        // 添加到会话映射
        sessionMap.put(sessionId, userSession);
        
        log.info("用户 {} (类型: {}) 建立WebSocket连接，会话ID: {}", userId, userType, sessionId);
    }
    
    /**
     * 移除用户会话
     */
    public void removeSession(String sessionId) {
        UserSession userSession = sessionMap.remove(sessionId);
        if (userSession != null) {
            String userId = userSession.getUserId();
            String userType = userSession.getUserType();
            
            // 从对应的映射中移除
            if ("guest".equals(userType)) {
                Map<String, UserSession> userSessionMap = contactSessions.get(userId);
                if (userSessionMap != null) {
                    userSessionMap.remove(sessionId);
                    if (userSessionMap.isEmpty()) {
                        contactSessions.remove(userId);
                    }
                }
            } else if ("agent".equals(userType)) {
                Map<String, UserSession> userSessionMap = agentSessions.get(userId);
                if (userSessionMap != null) {
                    userSessionMap.remove(sessionId);
                    if (userSessionMap.isEmpty()) {
                        agentSessions.remove(userId);
                    }
                }
            }
            
            log.info("用户 {} (类型: {}) 断开WebSocket连接，会话ID: {}", userId, userType, sessionId);
        }
    }
    
    /**
     * 向客人发送通知
     */
    public void sendNotificationToGuest(String contactId, String message) {
        Map<String, UserSession> userSessionMap = contactSessions.get(contactId);
        if (userSessionMap != null) {
            userSessionMap.values().forEach(session -> {
                try {
                    if (session.getWebSocketSession().isOpen()) {
                        session.getWebSocketSession().sendMessage(new TextMessage(message));
                        log.info("向客人 {} 发送通知: {}", contactId, message);
                    } else {
                        // 清理无效连接
                        removeSession(session.getSessionId());
                    }
                } catch (IOException e) {
                    log.error("向客人 {} 发送通知失败: {}", contactId, e.getMessage());
                    removeSession(session.getSessionId());
                }
            });
        } else {
            log.warn("向客人 {} 发送通知失败，chatwoot contactId未建立链接", contactId);
        }
    }
    
    /**
     * 向客服发送通知
     */
    public void sendNotificationToAgent(String agentId, String message) {
        Map<String, UserSession> userSessionMap = agentSessions.get(agentId);
        if (userSessionMap != null) {
            userSessionMap.values().forEach(session -> {
                try {
                    if (session.getWebSocketSession().isOpen()) {
                        session.getWebSocketSession().sendMessage(new TextMessage(message));
                        log.info("向客服 {} 发送通知: {}", agentId, message);
                    } else {
                        // 清理无效连接
                        removeSession(session.getSessionId());
                    }
                } catch (IOException e) {
                    log.error("向客服 {} 发送通知失败: {}", agentId, e.getMessage());
                    removeSession(session.getSessionId());
                }
            });
        } else {
            log.warn("向客服 {} 发送通知失败，chatwoot agentId未建立链接", agentId);
        }
    }

    /**
     * 向所有客服发送通知
     */
    public void sendNotificationToAllAgent(String message) {
        agentSessions.forEach((agentId, sessions) -> {
            sendNotificationToAgent(agentId, message);
        });
    }

    /**
     * 向用户发送通知（兼容旧接口）
     */
    public void sendNotificationToUser(String userId, String message) {
        // 尝试向客服发送
        sendNotificationToAgent(userId, message);
    }
    
    /**
     * 向会话中的所有用户发送通知（除了发送者）
     * 注意：这个方法现在主要用于会话状态更新，消息通知使用新的定向推送方法
     */
    public void sendNotificationToConversation(String conversationId, String senderId, String message) {
        // 这里需要根据conversationId获取相关用户列表
        // 暂时实现为向所有在线用户发送（除了发送者）
        // 遍历客人
        contactSessions.forEach((contactId, sessions) -> {
            if (!contactId.equals(senderId)) {
                sendNotificationToGuest(contactId, message);
            }
        });
        
        // 遍历客服
        agentSessions.forEach((agentId, sessions) -> {
            if (!agentId.equals(senderId)) {
                sendNotificationToAgent(agentId, message);
            }
        });
    }
    
    /**
     * 更新用户心跳
     */
    public void updateHeartbeat(String sessionId) {
        UserSession userSession = sessionMap.get(sessionId);
        if (userSession != null) {
            userSession.setLastHeartbeat(LocalDateTime.now());
        }
    }
    
    /**
     * 获取客人的所有会话
     */
    public Map<String, UserSession> getGuestSessions(String contactId) {
        return contactSessions.getOrDefault(contactId, new ConcurrentHashMap<>());
    }
    
    /**
     * 获取客服的所有会话
     */
    public Map<String, UserSession> getAgentSessions(String agentId) {
        return agentSessions.getOrDefault(agentId, new ConcurrentHashMap<>());
    }
    
    /**
     * 获取用户的所有会话（兼容旧接口）
     */
    public Map<String, UserSession> getUserSessions(String userId) {
        // 尝试获取客人会话
        Map<String, UserSession> guestSessions = getGuestSessions(userId);
        if (!guestSessions.isEmpty()) {
            return guestSessions;
        }
        
        // 尝试获取客服会话
        return getAgentSessions(userId);
    }
    
    /**
     * 检查客人是否在线
     */
    public boolean isGuestOnline(String contactId) {
        Map<String, UserSession> userSessionMap = contactSessions.get(contactId);
        if (userSessionMap != null) {
            return userSessionMap.values().stream()
                    .anyMatch(session -> session.getWebSocketSession().isOpen());
        }
        return false;
    }
    
    /**
     * 检查客服是否在线
     */
    public boolean isAgentOnline(String agentId) {
        Map<String, UserSession> userSessionMap = agentSessions.get(agentId);
        if (userSessionMap != null) {
            return userSessionMap.values().stream()
                    .anyMatch(session -> session.getWebSocketSession().isOpen());
        }
        return false;
    }
    
    /**
     * 检查用户是否在线（兼容旧接口）
     */
    public boolean isUserOnline(String userId) {
        return isGuestOnline(userId) || isAgentOnline(userId);
    }
    
    /**
     * 获取在线客人数量
     */
    public int getOnlineGuestCount() {
        return (int) contactSessions.values().stream()
                .flatMap(sessions -> sessions.values().stream())
                .filter(session -> session.getWebSocketSession().isOpen())
                .count();
    }
    
    /**
     * 获取在线客服数量
     */
    public int getOnlineAgentCount() {
        return (int) agentSessions.values().stream()
                .flatMap(sessions -> sessions.values().stream())
                .filter(session -> session.getWebSocketSession().isOpen())
                .count();
    }
    
    /**
     * 获取在线用户数量（兼容旧接口）
     */
    public int getOnlineUserCount() {
        return getOnlineGuestCount() + getOnlineAgentCount();
    }
    
    /**
     * 获取总连接数
     */
    public int getTotalConnectionCount() {
        return sessionMap.size();
    }
    
    /**
     * 清理过期连接
     */
    public void cleanupExpiredConnections() {
        LocalDateTime now = LocalDateTime.now();
        sessionMap.values().stream()
                .filter(session -> session.getLastHeartbeat().plusMinutes(5).isBefore(now))
                .forEach(session -> {
                    log.info("清理过期连接: 用户 {} (类型: {})，会话ID: {}", 
                            session.getUserId(), session.getUserType(), session.getSessionId());
                    removeSession(session.getSessionId());
                });
    }
    
    private String getClientIpAddress(WebSocketSession session) {
        String forwarded = session.getHandshakeHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        
        String realIp = session.getHandshakeHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        
        return session.getRemoteAddress() != null ? session.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
}
