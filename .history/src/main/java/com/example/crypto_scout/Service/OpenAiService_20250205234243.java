package com.example.crypto_scout.Service;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OpenAiService {
    private final String apiKey = "YOUR_OPENAI_API_KEY";
    private final String apiUrl = "https://api.openai.com/v1/chat/completions";

    public String analyzeCryptoTrends(String marketData) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders(null);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4");
        requestBody.put("messages", new Object[]{
            Map.of("role", "system", "content", "You are a crypto market analyst."),
            Map.of("role", "user", "content", marketData)
        });
        requestBody.put("max_tokens", 150);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().get("choices").toString();
        } else {
            return "Failed to analyze market data.";
        }
    }
}
