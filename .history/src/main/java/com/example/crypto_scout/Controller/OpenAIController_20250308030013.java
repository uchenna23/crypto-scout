package com.example.crypto_scout.Controller;

import com.example.crypto_scout.Service.OpenAIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/openai")
public class OpenAIController {

    private final OpenAIService openAIService;

    public OpenAIController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping("/chat")
    public ResponseEntity<String> chatWithBot(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query != null && (query.toLowerCase().contains("btc price") || query.toLowerCase().contains("bitcoin price"))) {
            String btcPrice = openAIService.getCurrentBtcPrice();
            return ResponseEntity.ok("The current price of Bitcoin is $" + btcPrice + " USD.");
        } else {
            String analysisResult = openAIService.analyzeMarketTrends(query);
            return ResponseEntity.ok(analysisResult);
        }
    }
}
