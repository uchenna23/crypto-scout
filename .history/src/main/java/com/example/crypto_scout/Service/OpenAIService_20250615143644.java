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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class OpenAIService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);

    private static final String CONTENT = "content";
    private static final String CHOICES = "choices";

    @Value("${openai.api.key}")
    private String openAiApiKey;

    // Using the Chat Completions endpoint for GPT‑4
    public static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

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
        // This method remains available for general analysis when no coin is explicitly targeted.
        Supplier<String> rateLimitedCall = io.github.resilience4j.ratelimiter.RateLimiter.decorateSupplier(rateLimiter, () -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            // Basic system prompt for general market analysis
            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4",
                    "messages", List.of(
                            Map.of("role", "system", CONTENT,
                                    "You are a comprehensive cryptocurrency market analyst with deep knowledge of historical trends, technical analysis, and market sentiment. " +
                                            "Provide a detailed analysis based on the provided data."),
                            Map.of("role", "user", CONTENT, cryptoData)
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
            if (response.getBody() != null && response.getBody().containsKey(CHOICES)) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get(CHOICES);
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    return message.get(CONTENT).toString();
                }
            }
            return "No analysis available.";
        });

        try {
            return rateLimitedCall.get();
        } catch (RequestNotPermitted ex) {
            return "Rate limit exceeded in service. Please try again later.";
        } catch (HttpClientErrorException ex) {
            logger.error("HTTP Status: {}", ex.getStatusCode());
            logger.error("Response Body: {}", ex.getResponseBodyAsString(), ex);
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return "OpenAI API quota exceeded. Please check your plan and billing details.";
            }
            return "API error: " + ex.getResponseBodyAsString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public String getCurrentCoinPrice(String coinId) {
        // Generalized method to fetch the current USD price of any coin using CoinGecko API
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId + "&vs_currencies=usd";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null) {
                Map<String, Object> coinData = (Map<String, Object>) responseBody.get(coinId);
                if (coinData != null && coinData.get("usd") != null) {
                    return coinData.get("usd").toString();
                }
            }
        } catch (Exception e) {
            return "Error fetching " + coinId + " price: " + e.getMessage();
        }
        return "Price not available";
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public String getCoinDetails(String coinId) {
        // Fetch detailed coin data from CoinGecko (price, market cap, symbol, etc.)
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId +
                "?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false&sparkline=false";
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> body = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && body != null) {
                return formatCoinDetails(body, coinId);
            }
        } catch (Exception e) {
            return "Error fetching details for " + coinId + ": " + e.getMessage();
        }
        return "Details not available for " + coinId;
    }

    @SuppressWarnings("unchecked")
    private String formatCoinDetails(Map<String, Object> body, String coinId) {
        String name = body.get("name") != null ? body.get("name").toString() : coinId;
        String symbol = body.get("symbol") != null ? body.get("symbol").toString().toUpperCase() : "";
        Map<String, Object> marketData = (Map<String, Object>) body.get("market_data");
        if (marketData != null) {
            Map<String, Object> currentPriceMap = (Map<String, Object>) marketData.get("current_price");
            Map<String, Object> marketCapMap = (Map<String, Object>) marketData.get("market_cap");
            String priceUsd = (currentPriceMap != null && currentPriceMap.get("usd") != null)
                    ? currentPriceMap.get("usd").toString() : "N/A";
            String marketCapUsd = (marketCapMap != null && marketCapMap.get("usd") != null)
                    ? marketCapMap.get("usd").toString() : "N/A";
            // You can extract more details as needed.
            return "Coin: %s (%s). Current Price: $%s USD. Market Cap: $%s USD.".formatted(
                    name, symbol, priceUsd, marketCapUsd);
        }
        return "Details not available for " + coinId;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public String analyzeCoin(String coinId) {
        // Fetch detailed coin information to provide context for analysis.
        String coinDetails = getCoinDetails(coinId);
        // Build a prompt that includes the coin details
        String prompt = "Based on the following data for " + coinId + ":\n" + coinDetails +
                "\nPlease provide a comprehensive analysis including historical trends, technical indicators, market sentiment, and potential future outlook for " + coinId + ".";
        
        Supplier<String> rateLimitedCall = RateLimiter.decorateSupplier(rateLimiter, () -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);

            // Use GPT‑4 with a system prompt tailored for comprehensive analysis.
            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4",
                    "messages", List.of(
                            Map.of("role", "system", CONTENT,
                                    "You are a comprehensive cryptocurrency market analyst with deep knowledge of historical trends, technical analysis, and market sentiment. " +
                                            "Based on the provided coin data, give a detailed and thoughtful analysis."),
                            Map.of("role", "user", CONTENT, prompt)
                    ),
                    "max_tokens", 300,
                    "temperature", 0.7
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    OPENAI_API_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );
            if (response.getBody() != null && response.getBody().containsKey(CHOICES)) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get(CHOICES);
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    return message.get(CONTENT).toString();
                }
            }
            return "No analysis available.";
        });

        try {
            return rateLimitedCall.get();
        } catch (RequestNotPermitted ex) {
            return "Rate limit exceeded in service. Please try again later.";
        } catch (HttpClientErrorException ex) {
            logger.error("HTTP Status: {}", ex.getStatusCode());
            logger.error("Response Body: {}", ex.getResponseBodyAsString(), ex);
            if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                return "OpenAI API quota exceeded. Please check your plan and billing details.";
            }
            return "API error: " + ex.getResponseBodyAsString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
