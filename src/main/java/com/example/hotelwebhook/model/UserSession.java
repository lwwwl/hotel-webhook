package com.example.hotelwebhook.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    private String userId;
    private String userType; // "guest" 或 "agent"
    private String sessionId;
    private WebSocketSession webSocketSession;
    private LocalDateTime connectedAt;
    private LocalDateTime lastHeartbeat;
    private String userAgent;
    private String ipAddress;
}
