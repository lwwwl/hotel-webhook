package com.example.hotelwebhook.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket连接响应模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketConnectionResponse {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * WebSocket连接URL
     */
    private String wsUrl;
    
    /**
     * WebSocket连接token
     */
    private String wsToken;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户类型 (guest/agent)
     */
    private String userType;
}
