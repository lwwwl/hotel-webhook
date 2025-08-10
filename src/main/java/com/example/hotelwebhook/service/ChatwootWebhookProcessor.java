package com.example.hotelwebhook.service;

import com.example.hotelwebhook.model.ChatwootEvent;
import com.example.hotelwebhook.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;

@Slf4j
@Service
public class ChatwootWebhookProcessor {
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * 处理Chatwoot webhook事件
     */
    public void processWebhookEvent(Map<String, Object> payload) {
        try {
            String eventType = extractEventType(payload);
            log.info("处理Chatwoot webhook事件: {}", eventType);
            
            ChatwootEvent event = parseEvent(payload, eventType);
            log.info("ChatwootEvent 生成结果：{}", JsonUtil.toJson(event));
            if (event != null) {
                notificationService.processEvent(event);
            }
        } catch (Exception e) {
            log.error("处理webhook事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 解析事件类型
     */
    private String extractEventType(Map<String, Object> payload) {
        // Chatwoot webhook事件类型通常在payload的event字段中
        Object eventObj = payload.get("event");
        if (eventObj != null) {
            return eventObj.toString();
        }
        
        return "unknown";
    }
    
    /**
     * 解析事件数据
     */
    private ChatwootEvent parseEvent(Map<String, Object> payload, String eventType) {
        try {
            ChatwootEvent.ChatwootEventBuilder builder = ChatwootEvent.builder()
                    .eventType(eventType)
                    .timestamp(System.currentTimeMillis());

            return switch (eventType) {
                case "message_created" -> parseMessageCreatedEvent(payload, builder);
                case "conversation_created" -> parseConversationCreatedEvent(payload, builder);
                case "conversation_updated" -> parseConversationUpdatedEvent(payload, builder);
                case "conversation_resolved" -> parseConversationResolvedEvent(payload, builder);
                default -> {
                    log.warn("未知的事件类型: {}", eventType);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("解析事件数据失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 解析消息创建事件
     */
    private ChatwootEvent parseMessageCreatedEvent(Map<String, Object> payload, ChatwootEvent.ChatwootEventBuilder builder) {
        @SuppressWarnings("unchecked")
        Map<String, Object> conversation = (Map<String, Object>) payload.get("conversation");
        
        // 优先使用 conversation.messages[0] 作为实际消息体
        String messageJson = null;
        if (conversation != null) {
            Object messagesObj = conversation.get("messages");
            if (messagesObj instanceof List) {
                List<?> messages = (List<?>) messagesObj;
                if (!messages.isEmpty() && messages.getFirst() instanceof Map) {
                    messageJson = JsonUtil.toJson(messages.getFirst());
                }
            }
        }

        // 提取消息类型
        String messageType = extractString(payload, "message_type");
        
        // 提取接收者信息
        String recipientId = null;
        String recipientType = null;
        
        if ("incoming".equals(messageType)) {
            // 客人向客服发送消息，接收者是客服
            recipientId = extractRecipientIdFromConversation(conversation, "assignee");
            recipientType = "agent";
        } else if ("outgoing".equals(messageType)) {
            // 客服向客人发送消息，接收者是客人
            recipientId = extractRecipientIdFromConversation(conversation, "sender");
            recipientType = "guest";
        } else {
            return null;
        }
        
        return builder
                .conversationId(extractString(conversation, "id"))
                .messageType(messageType)
                .recipientId(recipientId)
                .recipientType(recipientType)
                .metadata(messageJson)
                .build();
    }
    
    /**
     * 解析会话创建事件
     */
    private ChatwootEvent parseConversationCreatedEvent(Map<String, Object> payload, ChatwootEvent.ChatwootEventBuilder builder) {
        @SuppressWarnings("unchecked")
        Map<String, Object> conversation = (Map<String, Object>) payload.get("conversation");
        if (conversation == null) {
            return null;
        }
        
        return builder
                .conversationId(extractString(conversation, "id"))
                .build();
    }
    
    /**
     * 解析会话更新事件
     */
    private ChatwootEvent parseConversationUpdatedEvent(Map<String, Object> payload, ChatwootEvent.ChatwootEventBuilder builder) {
        @SuppressWarnings("unchecked")
        Map<String, Object> conversation = (Map<String, Object>) payload.get("conversation");
        if (conversation == null) {
            return null;
        }
        
        return builder
                .conversationId(extractString(conversation, "id"))
                .build();
    }
    
    /**
     * 解析会话解决事件
     */
    private ChatwootEvent parseConversationResolvedEvent(Map<String, Object> payload, ChatwootEvent.ChatwootEventBuilder builder) {
        @SuppressWarnings("unchecked")
        Map<String, Object> conversation = (Map<String, Object>) payload.get("conversation");
        if (conversation == null) {
            return null;
        }
        
        return builder
                .conversationId(extractString(conversation, "id"))
                .build();
    }
    
    /**
     * 安全提取字符串值
     */
    private String extractString(Map<String, Object> map, String key) {
        Object value = map != null ? map.get(key) : null;
        return value != null ? value.toString() : null;
    }

    /**
     * 从会话信息中提取接收者ID
     */
    private String extractRecipientIdFromConversation(Map<String, Object> conversation, String fieldName) {
        if (conversation == null) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) conversation.get("meta");
        if (meta == null) {
            return null;
        }
        
        Object fieldValue = meta.get(fieldName);
        if (fieldValue == null) {
            return null;
        }
        
        if (fieldValue instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldMap = (Map<String, Object>) fieldValue;
            return extractString(fieldMap, "id");
        }
        
        return fieldValue.toString();
    }
}
