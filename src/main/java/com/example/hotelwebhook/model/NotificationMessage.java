package com.example.hotelwebhook.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String type; // "new_message", "conversation_created", "conversation_updated", "typing", etc.
    private String conversationId;
    private String messageId;
    private String senderId;
    private String content;
    private Long timestamp;
    private Object data; // 额外的数据
}
