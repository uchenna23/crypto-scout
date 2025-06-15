package com.example.crypto_scout;

import com.example.crypto_scout.Service.OpenAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenAIServiceTest {

    private OpenAIService service;
    private RestTemplate restTemplateMock;

    @BeforeEach
    void setUp() {
        // Use reflection to inject a mock RestTemplate
        service = new OpenAIService();
        restTemplateMock = mock(RestTemplate.class);
        try {
            var restTemplateField = OpenAIService.class.getDeclaredField("restTemplate");
            restTemplateField.setAccessible(true);
            restTemplateField.set(service, restTemplateMock);

            // Optionally set the API key for tests
            var apiKeyField = OpenAIService.class.getDeclaredField("openAiApiKey");
            apiKeyField.setAccessible(true);
            apiKeyField.set(service, "test-key");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void analyzeMarketTrends_returnsAnalysisOnSuccess() {
        // Arrange
        Map<String, Object> message = Map.of("content", "Market looks bullish.");
        Map<String, Object> choice = Map.of("message", message);
        Map<String, Object> responseBody = Map.of("choices", List.of(choice));
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplateMock.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);

        // Act
        String result = service.analyzeMarketTrends("BTC data");

        // Assert
        assertEquals("Market looks bullish.", result);
    }

    @Test
    void analyzeMarketTrends_returnsNoAnalysisIfChoicesEmpty() {
        Map<String, Object> responseBody = Map.of("choices", List.of());
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        String result = service.analyzeMarketTrends("BTC data");
        assertEquals("No analysis available.", result);
    }

    @Test
    void analyzeMarketTrends_returnsErrorOnHttpException() {
        when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

        String result = service.analyzeMarketTrends("BTC data");
        assertTrue(result.startsWith("API error:") || result.startsWith("Error:"));
    }

    @Test
    void getCurrentCoinPrice_returnsPriceOnSuccess() {
        Map<String, Object> coinData = Map.of("usd", 12345.67);
        Map<String, Object> responseBody = Map.of("bitcoin", coinData);
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplateMock.getForEntity(anyString(), eq(Map.class))).thenReturn(responseEntity);

        String result = service.getCurrentCoinPrice("bitcoin");
        assertEquals("12345.67", result);
    }

    @Test
    void getCurrentCoinPrice_returnsErrorOnException() {
        when(restTemplateMock.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("API down"));

        String result = service.getCurrentCoinPrice("bitcoin");
        assertTrue(result.contains("Error fetching bitcoin price"));
    }

    @Test
    void getCoinDetails_returnsFormattedDetailsOnSuccess() {
        Map<String, Object> currentPrice = Map.of("usd", 100.0);
        Map<String, Object> marketCap = Map.of("usd", 1000000.0);
        Map<String, Object> marketData = Map.of("current_price", currentPrice, "market_cap", marketCap);
        Map<String, Object> body = Map.of("name", "Bitcoin", "symbol", "btc", "market_data", marketData);
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);

        when(restTemplateMock.getForEntity(anyString(), eq(Map.class))).thenReturn(responseEntity);

        String result = service.getCoinDetails("bitcoin");
        assertTrue(result.contains("Coin: Bitcoin (BTC). Current Price: $100.0 USD. Market Cap: $1000000.0 USD."));
    }

    @Test
    void getCoinDetails_returnsErrorOnException() {
        when(restTemplateMock.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("API down"));

        String result = service.getCoinDetails("bitcoin");
        assertTrue(result.contains("Error fetching details for bitcoin"));
    }
}