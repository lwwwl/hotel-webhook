package com.example.hotelwebhook.controller;

import com.example.hotelwebhook.service.ChatwootWebhookProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chatwoot-webhook")
public class WebhookController {
    
    @Autowired
    private ChatwootWebhookProcessor webhookProcessor;
    
    @PostMapping("/callback")
    public ResponseEntity<String> handleChatwootWebhook(@RequestBody Map<String, Object> payload) {
        try {
            log.info("收到Chatwoot webhook回调: {}", payload);
            webhookProcessor.processWebhookEvent(payload);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("处理webhook回调失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("error");
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
} 