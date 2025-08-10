package com.example.hotelwebhook.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebSocketHeartbeatTask {
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    /**
     * 每5分钟清理一次过期连接
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void cleanupExpiredConnections() {
        try {
            int beforeCount = sessionManager.getTotalConnectionCount();
            sessionManager.cleanupExpiredConnections();
            int afterCount = sessionManager.getTotalConnectionCount();
            
            if (beforeCount != afterCount) {
                log.info("清理过期连接完成: {} -> {}", beforeCount, afterCount);
            }
        } catch (Exception e) {
            log.error("清理过期连接失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 每分钟记录一次连接统计信息
     */
    @Scheduled(fixedRate = 60000) // 1分钟
    public void logConnectionStats() {
        try {
            int onlineUsers = sessionManager.getOnlineUserCount();
            int totalConnections = sessionManager.getTotalConnectionCount();
            
            if (onlineUsers > 0 || totalConnections > 0) {
                log.info("连接统计 - 在线用户: {}, 总连接数: {}", onlineUsers, totalConnections);
            }
        } catch (Exception e) {
            log.error("记录连接统计失败: {}", e.getMessage(), e);
        }
    }
} 