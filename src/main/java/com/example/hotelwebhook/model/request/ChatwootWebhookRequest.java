package com.example.hotelwebhook.model.request;

import lombok.Data;

@Data
public class ChatwootWebhookRequest {
    private String userId;
    // 可根据实际 webhook payload 添加更多字段
} 