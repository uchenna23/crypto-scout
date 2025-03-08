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
        
        // Convert the query to lower-case for easier matching
        String lowerQuery = query.toLowerCase();

        // Check for price-related keywords
        boolean mentionsPrice = lowerQuery.contains("price") ||
                                  lowerQuery.contains("cost") ||
                                  lowerQuery.contains("value") ||
                                  lowerQuery.contains("quote") ||
                                  lowerQuery.contains("current") ||
                                  lowerQuery.contains("how much");

        // Attempt to extract a coin id from the query using a helper method
        String coinId = extractCoinId(lowerQuery);
        
        // If a coin is mentioned and price info is requested, get its current price
        if (coinId != null && mentionsPrice) {
            String coinPrice = openAIService.getCurrentCoinPrice(coinId);
            return ResponseEntity.ok("The current price of " + coinId + " is $" + coinPrice + " USD.");
        } else {
            // Otherwise, use the OpenAI service for market analysis or general questions
            String analysisResult = openAIService.analyzeMarketTrends(query);
            return ResponseEntity.ok(analysisResult);
        }
    }
    
    /**
     * Helper method that checks for known coin aliases in the query
     * and returns the corresponding coin id (as used by CoinGecko).
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
