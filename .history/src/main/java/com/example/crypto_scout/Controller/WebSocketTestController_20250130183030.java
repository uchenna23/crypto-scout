package com.example.crypto_scout.Controller;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class WebSocketTestController {
    
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketTestController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/send")
    public String sendTestMessage() {
        Map<String, Double> testMessage = Map.of("BTC-USD", 45000.12);
        messagingTemplate.convertAndSend("/topic/crypto", testMessage);
        return "Test message sent!";
    }
}

