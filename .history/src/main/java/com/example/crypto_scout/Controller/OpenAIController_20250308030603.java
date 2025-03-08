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
        // Retrieve the query from either "query" or fallback to "cryptoData"
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            query = request.get("cryptoData");
        }

        // Convert the query to lower case for easier matching
        String lowerQuery = query.toLowerCase();

        // Check if the query mentions Bitcoin (or BTC) and includes any price-related keywords
        boolean mentionsCrypto = lowerQuery.contains("bitcoin") || lowerQuery.contains("btc");
        boolean mentionsPrice = lowerQuery.contains("price") ||
                                  lowerQuery.contains("cost") ||
                                  lowerQuery.contains("value") ||
                                  lowerQuery.contains("quote") ||
                                  lowerQuery.contains("current");
        
        if (mentionsCrypto && mentionsPrice) {
            String btcPrice = openAIService.getCurrentBtcPrice();
            return ResponseEntity.ok("The current price of Bitcoin is $" + btcPrice + " USD.");
        } else {
            // Otherwise, use the OpenAI service for market analysis or general questions.
            String analysisResult = openAIService.analyzeMarketTrends(query);
            return ResponseEntity.ok(analysisResult);
        }
    }
}
