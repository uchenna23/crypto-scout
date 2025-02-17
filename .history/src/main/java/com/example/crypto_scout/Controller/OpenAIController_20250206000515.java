package com.example.crypto_scout.Controller;

import com.example.crypto_scout.Service.OpenAIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/market-analysis")
@CrossOrigin(origins = "*")
public class OpenAIController {

    private final OpenAIService openAIService;

    public OpenAIController(OpenAIService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeMarket(@RequestBody Map<String, String> request) {
        String cryptoData = request.get("cryptoData");
        String analysisResult = openAIService.analyzeMarketTrends(cryptoData);
        return ResponseEntity.ok(analysisResult);
    }
}
