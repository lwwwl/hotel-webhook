package com.example.hotelwebhook.service;

import java.time.LocalDateTime;

import com.example.hotelwebhook.utils.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.hotelwebhook.model.ChatwootEvent;
import com.example.hotelwebhook.model.NotificationMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationService {
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 处理Chatwoot事件
     */
    public void processEvent(ChatwootEvent event) {
        try {
            NotificationMessage notification = createNotificationMessage(event);
            log.info("执行到 processEvent event: {}", JsonUtil.toJson(notification));
            if (notification != null) {
                sendNotification(event, notification);
            }
        } catch (Exception e) {
            log.error("处理事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 创建通知消息（非 message_created 场景）
     */
    private NotificationMessage createNotificationMessage(ChatwootEvent event) {
        NotificationMessage.NotificationMessageBuilder builder = NotificationMessage.builder()
                .timestamp(System.currentTimeMillis())
                .conversationId(event.getConversationId());

        return switch (event.getEventType()) {
            case "message_created" -> builder
                    .type("message_created")
                    .data(event.getMetadata())
                    .build();
            case "message_updated" -> builder
                    .type("message_updated")
                    .data(event.getMetadata())
                    .build();
            case "conversation_created" -> builder
                    .type("conversation_created")
                    .data(event.getMetadata())
                    .build();
            case "conversation_updated" -> builder
                    .type("conversation_updated")
                    .data(event.getMetadata())
                    .build();
            case "conversation_resolved" -> builder
                    .type("conversation_resolved")
                    .build();
            default -> {
                log.warn("未知的事件类型: {}", event.getEventType());
                yield null;
            }
        };
    }
    
    /**
     * 发送通知
     */
    private void sendNotification(ChatwootEvent event, NotificationMessage notification) {
        try {
            String notificationJson = objectMapper.writeValueAsString(notification);
            log.info("执行到 sendNotification notification: {}", notificationJson);
            // 根据事件类型决定通知策略
            switch (event.getEventType()) {
                case "message_created":
                    sendMessageNotification(event, notificationJson);
                    break;
                    
                case "conversation_created":
                    // 向相关客服发送新会话通知（这里仍保留原有策略）
                    sendConversationNotification(event, notificationJson);
                    break;
                    
                case "conversation_updated":
                case "conversation_resolved":
                    // 向会话参与者发送会话状态更新通知
                    sendConversationStatusNotification(event, notificationJson);
                    break;
            }
        } catch (JsonProcessingException e) {
            log.error("序列化通知消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发送消息通知（定向推送）
     */
    private void sendMessageNotification(ChatwootEvent event, String notificationJson) {
        // 检查是否有接收者信息
        if (event.getRecipientId() == null || event.getRecipientType() == null) {
            log.warn("消息事件缺少接收者信息，无法发送定向通知: conversationId={}, messageType={}", 
                    event.getConversationId(), event.getMessageType());
            return;
        }
        
        // 根据接收者类型发送通知
        if ("guest".equals(event.getRecipientType())) {
            // 向客人发送通知
            sessionManager.sendNotificationToGuest(event.getRecipientId(), notificationJson);
            log.info("向客人 {} 发送消息通知: conversationId={}", 
                    event.getRecipientId(), event.getConversationId());
        } else if ("agent".equals(event.getRecipientType())) {
            // 向客服发送通知
            sessionManager.sendNotificationToAgent(event.getRecipientId(), notificationJson);
            log.info("向客服 {} 发送消息通知: conversationId={}", 
                    event.getRecipientId(), event.getConversationId());
        } else {
            log.warn("未知的接收者类型: {}, 无法发送通知", event.getRecipientType());
        }
    }
    
    /**
     * 发送会话通知
     */
    private void sendConversationNotification(ChatwootEvent event, String notificationJson) {
        // 向所有在线客服发送新会话通知
        // 这里可以根据inboxId或其他条件筛选客服
        sessionManager.sendNotificationToUser("all_agents", notificationJson);
    }
    
    /**
     * 发送会话状态通知
     */
    private void sendConversationStatusNotification(ChatwootEvent event, String notificationJson) {
        // 向会话参与者发送状态更新通知
        if (event.getConversationId() != null) {
            sessionManager.sendNotificationToConversation(
                event.getConversationId(), 
                null, 
                notificationJson
            );
        }
    }
}
