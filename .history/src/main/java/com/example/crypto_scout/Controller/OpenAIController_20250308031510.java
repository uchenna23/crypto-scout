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
        
        // Convert the query to lower-case for matching
        String lowerQuery = query.toLowerCase();

        // Check for a range of coin-detail keywords
        boolean mentionsDetails = lowerQuery.contains("price") ||
                                  lowerQuery.contains("cost") ||
                                  lowerQuery.contains("value") ||
                                  lowerQuery.contains("quote") ||
                                  lowerQuery.contains("current") ||
                                  lowerQuery.contains("market cap") ||
                                  lowerQuery.contains("symbol") ||
                                  lowerQuery.contains("how much");
        
        // Attempt to extract a coin id from the query using a helper method
        String coinId = extractCoinId(lowerQuery);
        
        if (coinId != null && mentionsDetails) {
            // Use the service method to fetch detailed coin information
            String coinDetails = openAIService.getCoinDetails(coinId);
            return ResponseEntity.ok(coinDetails);
        } else {
            // Otherwise, use the OpenAI service for market analysis or general questions
            String analysisResult = openAIService.analyzeMarketTrends(query);
            return ResponseEntity.ok(analysisResult);
        }
    }
    
    /**
     * Helper method to extract a coin id from the query using known coin aliases.
     * The keys represent common query fragments and the values are the CoinGecko coin ids.
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
