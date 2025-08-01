package com.example.hotelwebhook.service;

import com.example.hotelwebhook.model.request.ChatwootWebhookRequest;
import com.example.hotelwebhook.websocket.NotifyWebSocketHandler;
import org.springframework.stereotype.Service;

@Service
public class WebhookService {
    public void handleChatwootWebhook(ChatwootWebhookRequest request) {
        // 这里假设 userId 字段在 webhook payload 中
        String userId = request.getUserId();
        // 通知前端有新消息
        NotifyWebSocketHandler.sendNotify(userId);
    }
} 