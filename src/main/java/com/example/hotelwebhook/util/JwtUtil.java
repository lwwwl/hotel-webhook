package com.example.hotelwebhook.util;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 简化的Token工具类
 * 用于生成简单的连接标识
 */
@Slf4j
@Component
public class JwtUtil {
    
    /**
     * 生成简单的连接标识
     */
    public String generateConnectionId(String userId, String userType) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 构建连接标识
            String connectionData = String.format("%s:%s:%s", 
                    userId, userType, 
                    now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // Base64编码
            return Base64.getEncoder().encodeToString(connectionData.getBytes(StandardCharsets.UTF_8));
            
        } catch (Exception e) {
            log.error("生成连接标识失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 从连接标识中提取用户ID
     */
    public String extractUserId(String connectionId) {
        try {
            if (connectionId == null || connectionId.isEmpty()) {
                return null;
            }
            
            // Base64解码
            String decodedData = new String(Base64.getDecoder().decode(connectionId), StandardCharsets.UTF_8);
            String[] parts = decodedData.split(":");
            
            if (parts.length >= 1) {
                return parts[0];
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("从连接标识中提取用户ID失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从连接标识中提取用户类型
     */
    public String extractUserType(String connectionId) {
        try {
            if (connectionId == null || connectionId.isEmpty()) {
                return null;
            }
            
            // Base64解码
            String decodedData = new String(Base64.getDecoder().decode(connectionId), StandardCharsets.UTF_8);
            String[] parts = decodedData.split(":");
            
            if (parts.length >= 2) {
                return parts[1];
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("从连接标识中提取用户类型失败: {}", e.getMessage());
            return null;
        }
    }
}
