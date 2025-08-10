package com.example.hotelwebhook.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotelwebhook.model.response.WebSocketConnectionResponse;
import com.example.hotelwebhook.service.WebSocketSessionManager;
import com.example.hotelwebhook.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/websocket")
@CrossOrigin(origins = "*")
public class WebSocketController {
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Value("${websocket.server.url:ws://localhost:7766}")
    private String websocketServerUrl;
    
    /**
     * 获取客服端WebSocket连接信息
     */
    @PostMapping("/connect/agent")
    public ResponseEntity<WebSocketConnectionResponse> getAgentWebSocketConnection(
            @RequestParam String userId) {
        
        try {
            // 生成连接标识
            String connectionId = jwtUtil.generateConnectionId(userId, "agent");
            
            // 构建WebSocket连接URL
            String wsUrl = buildWebSocketUrl("userId", userId, connectionId);
            
            WebSocketConnectionResponse response = WebSocketConnectionResponse.builder()
                    .success(true)
                    .message("客服端WebSocket连接信息获取成功")
                    .wsUrl(wsUrl)
                    .wsToken(connectionId)
                    .userId(userId)
                    .userType("agent")
                    .build();
            
            log.info("客服 {} 获取WebSocket连接信息", userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取客服端WebSocket连接信息失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    WebSocketConnectionResponse.builder()
                            .success(false)
                            .message("获取客服端WebSocket连接信息失败: " + e.getMessage())
                            .build()
            );
        }
    }
    
    /**
     * 获取客人端WebSocket连接信息
     */
    @PostMapping("/connect/guest")
    public ResponseEntity<WebSocketConnectionResponse> getGuestWebSocketConnection(
            @RequestParam String guestId) {
        
        try {
            // 生成连接标识
            String connectionId = jwtUtil.generateConnectionId(guestId, "guest");
            
            // 构建WebSocket连接URL
            String wsUrl = buildWebSocketUrl("guestId", guestId, connectionId);
            
            WebSocketConnectionResponse response = WebSocketConnectionResponse.builder()
                    .success(true)
                    .message("客人端WebSocket连接信息获取成功")
                    .wsUrl(wsUrl)
                    .wsToken(connectionId)
                    .userId(guestId)
                    .userType("guest")
                    .build();
            
            log.info("客人 {} 获取WebSocket连接信息", guestId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取客人端WebSocket连接信息失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    WebSocketConnectionResponse.builder()
                            .success(false)
                            .message("获取客人端WebSocket连接信息失败: " + e.getMessage())
                            .build()
            );
        }
    }
    
    /**
     * 检查用户在线状态
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> getUserStatus(@PathVariable String userId) {
        try {
            boolean isGuestOnline = sessionManager.isGuestOnline(userId);
            boolean isAgentOnline = sessionManager.isAgentOnline(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("isGuestOnline", isGuestOnline);
            response.put("isAgentOnline", isAgentOnline);
            response.put("isOnline", isGuestOnline || isAgentOnline);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("检查用户状态失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "检查用户状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取在线用户统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOnlineStats() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("onlineGuestCount", sessionManager.getOnlineGuestCount());
            response.put("onlineAgentCount", sessionManager.getOnlineAgentCount());
            response.put("totalConnectionCount", sessionManager.getTotalConnectionCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取在线统计失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取在线统计失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 构建WebSocket连接URL
     */
    private String buildWebSocketUrl(String paramName, String paramValue, String connectionId) {
        return String.format("%s/ws/notify?%s=%s&connectionId=%s", 
                websocketServerUrl, paramName, paramValue, connectionId);
    }
}
