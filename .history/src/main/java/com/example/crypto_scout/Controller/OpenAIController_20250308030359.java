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
        // Try to get the query from "query" key; if not available, fallback to "cryptoData"
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            query = request.get("cryptoData");
        }
        
        // If the query contains keywords for BTC price, call the BTC price service method
        if (query != null && (query.toLowerCase().contains("btc price") || query.toLowerCase().contains("bitcoin price"))) {
            String btcPrice = openAIService.getCurrentBtcPrice();
            return ResponseEntity.ok("The current price of Bitcoin is $" + btcPrice + " USD.");
        } else {
            // Otherwise, perform market analysis using OpenAI
            String analysisResult = openAIService.analyzeMarketTrends(query);
            return ResponseEntity.ok(analysisResult);
        }
    }
}
