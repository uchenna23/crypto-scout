package com.example.crypto_scout.Service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    // Using the Chat Completions endpoint for GPT‑4
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    private final RateLimiter rateLimiter;
    private final RestTemplate restTemplate = new RestTemplate();

    public OpenAIService() {
        // Configure rate limiter: max 10 requests per minute, waiting up to 1 second for a slot
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(10)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ofSeconds(1))
                .build();

        this.rateLimiter = RateLimiterRegistry.of(config).rateLimiter("openaiLimiter");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public String analyzeMarketTrends(String cryptoData) {
        Supplier<String> rateLimitedCall = RateLimiter.decorateSupplier(rateLimiter, () -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            // Updated request body for the Chat Completions API using GPT‑4
            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4",
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "You are a helpful cryptocurrency assistant. " +
                                    "You answer general questions about cryptocurrency, such as current prices, market capitalization, trends, and other relevant data. " +
                                    "When provided with detailed market data, you also perform a market analysis and provide insights."),
                            Map.of("role", "user", "content", cryptoData)
                    ),
                    "max_tokens", 150,
                    "temperature", 0.7
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    OPENAI_API_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    return message.get("content").toString();
                }
            }
            return "No analysis available.";
        });

        try {
            return rateLimitedCall.get();
        } catch (RequestNotPermitted ex) {
            return "Rate limit exceeded in service. Please try again later.";
        } catch (HttpClientErrorException ex) {
            System.err.println("HTTP Status: " + ex.getStatusCode());
            System.err.println("Response Body: " + ex.getResponseBodyAsString());
            ex.printStackTrace();
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return "OpenAI API quota exceeded. Please check your plan and billing details.";
            }
            return "API error: " + ex.getResponseBodyAsString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // New method to fetch current Bitcoin price using the CoinGecko API
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public String getCurrentCoinPrice(String coinId) {
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId + "&vs_currencies=usd";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> coinData = (Map<String, Object>) response.getBody().get(coinId);
            if (coinData != null && coinData.get("usd") != null) {
                return coinData.get("usd").toString();
            }
        }
        } catch (Exception e) {
            return "Error fetching " + coinId + " price: " + e.getMessage();
        }
        return "Price not available";
    }

}
