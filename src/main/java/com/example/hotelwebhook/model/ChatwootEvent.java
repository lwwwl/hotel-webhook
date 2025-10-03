package com.example.hotelwebhook.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatwootEvent {
    private String eventType; // "message_created", "conversation_created", "conversation_updated", etc.
    private String conversationId;
    private String metadata;
    private Long timestamp;

    // 新增字段用于消息定向推送
    private String messageType; // "incoming" 或 "outgoing"
    private String recipientId; // 接收者ID (contactId 或 userId)
    private String recipientType; // "guest" 或 "agent"
}
