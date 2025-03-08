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
        
        // Convert query to lower case for matching
        String lowerQuery = query.toLowerCase();
        
        // Attempt to extract a coin id using known aliases
        String coinId = extractCoinId(lowerQuery);
        
        // Determine what type of request this is
        if (coinId != null) {
            // If the query includes "analyze" or "analysis", perform a comprehensive analysis.
            if (lowerQuery.contains("analyze") || lowerQuery.contains("analysis")) {
                String analysis = openAIService.analyzeCoin(coinId);
                return ResponseEntity.ok(analysis);
            }
            // Otherwise, if the query is asking for basic coin details (price, market cap, symbol, etc.)
            boolean mentionsDetails = lowerQuery.contains("price") ||
                                      lowerQuery.contains("cost") ||
                                      lowerQuery.contains("value") ||
                                      lowerQuery.contains("quote") ||
                                      lowerQuery.contains("current") ||
                                      lowerQuery.contains("market cap") ||
                                      lowerQuery.contains("symbol") ||
                                      lowerQuery.contains("how much");
            if (mentionsDetails) {
                String details = openAIService.getCoinDetails(coinId);
                return ResponseEntity.ok(details);
            }
        }
        // If no coin-specific query is detected, fall back to general analysis using OpenAI.
        String analysisResult = openAIService.analyzeMarketTrends(query);
        return ResponseEntity.ok(analysisResult);
    }
    
    /**
     * Helper method to extract a coin id from the query using known coin aliases.
     */
    private String extractCoinId(String query) {
        Map<String, String> coinAliases = Map.of(
                "btc", "bitcoin",
                "bitcoin", "bitcoin",
                "eth", "ethereum",
                "ethereum", "ethereum",
                "doge", "dogecoin",
                "dogecoin", "dogecoin",
                "ltc", "litecoin",
                "litecoin", "litecoin"
                // Add more coin aliases as needed
        );
        for (Map.Entry<String, String> entry : coinAliases.entrySet()) {
            if (query.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
