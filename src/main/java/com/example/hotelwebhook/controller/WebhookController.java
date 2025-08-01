package com.example.hotelwebhook.controller;

import com.example.hotelwebhook.model.request.ChatwootWebhookRequest;
import com.example.hotelwebhook.service.WebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class WebhookController {
    @Autowired
    private WebhookService webhookService;

    @PostMapping("/chatwoot")
    public void handleChatwootWebhook(@RequestBody ChatwootWebhookRequest request) {
        webhookService.handleChatwootWebhook(request);
    }
} 