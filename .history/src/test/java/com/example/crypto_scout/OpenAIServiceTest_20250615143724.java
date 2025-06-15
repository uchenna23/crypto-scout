package com.example.crypto_scout;

import com.example.crypto_scout.Service.OpenAIService;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OpenAIServiceTest {

    private OpenAIService service;
    private RestTemplate restTemplateMock;

    @BeforeEach
    void setUp() throws Exception {
        service = new OpenAIService();
        restTemplateMock = mock(RestTemplate.class);
        // inject RestTemplate
        var restField = OpenAIService.class.getDeclaredField("restTemplate");
        restField.setAccessible(true);
        restField.set(service, restTemplateMock);
        // inject API key
        var apiField = OpenAIService.class.getDeclaredField("openAiApiKey");
        apiField.setAccessible(true);
        apiField.set(service, "test-key");
    }

    // --- analyzeMarketTrends tests ---
    @Test
    void analyzeMarketTrends_success() {
        Map<String,Object> choice = Map.of("message", Map.of("content", "analysis-result"));
        ResponseEntity<Map> resp = new ResponseEntity<>(Map.of("choices", List.of(choice)), HttpStatus.OK);
        when(restTemplateMock.exchange(eq(OpenAIService.OPENAI_API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(resp);
        String result = service.analyzeMarketTrends("data");
        assertEquals("analysis-result", result);
    }

    @Test
    void analyzeMarketTrends_noChoices() {
        ResponseEntity<Map> resp = new ResponseEntity<>(Map.of("choices", List.of()), HttpStatus.OK);
        when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class))).thenReturn(resp);
        assertEquals("No analysis available.", service.analyzeMarketTrends("data"));
    }

    @Test
void analyzeMarketTrends_rateLimit() {
    when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
        .thenThrow(new Exception("Rate limit exceeded"));
    assertEquals("Rate limit exceeded in service. Please try again later.",
        service.analyzeMarketTrends("data"));
}

    @Test
    void analyzeMarketTrends_http429() {
        HttpClientErrorException ex = HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS, "Too Many", null,
            "quota body".getBytes(), StandardCharsets.UTF_8);
        when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class))).thenThrow(ex);
        assertEquals("OpenAI API quota exceeded. Please check your plan and billing details.",
            service.analyzeMarketTrends("data"));
    }

    @Test
    void analyzeMarketTrends_httpErrorOther() {
        HttpClientErrorException ex = HttpClientErrorException.create(
            HttpStatus.BAD_REQUEST, "Bad", null,
            "error-body".getBytes(), StandardCharsets.UTF_8);
        when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class))).thenThrow(ex);
        assertEquals("API error: error-body", service.analyzeMarketTrends("data"));
    }

    @Test
    void analyzeMarketTrends_exception() {
        when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
            .thenThrow(new RuntimeException("fail-trend"));
        assertEquals("Error: fail-trend", service.analyzeMarketTrends("data"));
    }

    // --- getCurrentCoinPrice tests ---
    @Test
    void getCurrentCoinPrice_success() {
        Map<String,Object> coinData = Map.of("usd", 123);
        ResponseEntity<Map> resp = new ResponseEntity<>(Map.of("bitcoin", coinData), HttpStatus.OK);
        when(restTemplateMock.getForEntity(contains("simple/price"), eq(Map.class))).thenReturn(resp);
        assertEquals("123", service.getCurrentCoinPrice("bitcoin"));
    }

    @Test
    void getCurrentCoinPrice_noData() {
        when(restTemplateMock.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(Map.of(), HttpStatus.OK));
        assertEquals("Price not available", service.getCurrentCoinPrice("bitcoin"));
    }

    @Test
    void getCurrentCoinPrice_exception() {
        when(restTemplateMock.getForEntity(anyString(), eq(Map.class)))
            .thenThrow(new RuntimeException("fail-price"));
        String res = service.getCurrentCoinPrice("bitcoin");
        assertTrue(res.contains("Error fetching bitcoin price: fail-price"));
    }

    // --- getCoinDetails tests ---
    @Test
    void getCoinDetails_success() {
        Map<String,Object> priceMap = Map.of("usd", 10);
        Map<String,Object> capMap = Map.of("usd", 1000);
        Map<String,Object> marketData = Map.of("current_price", priceMap, "market_cap", capMap);
        Map<String,Object> body = Map.of("name", "Bitcoin", "symbol", "btc", "market_data", marketData);
        when(restTemplateMock.getForEntity(contains("/coins/bitcoin"), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
        String details = service.getCoinDetails("bitcoin");
        assertTrue(details.contains("Coin: Bitcoin (BTC). Current Price: $10 USD. Market Cap: $1000 USD."));
    }

    @Test
    void getCoinDetails_noDetails() {
        when(restTemplateMock.getForEntity(anyString(), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(Map.of(), HttpStatus.OK));
        assertEquals("Details not available for bitcoin", service.getCoinDetails("bitcoin"));
    }

    @Test
    void getCoinDetails_exception() {
        when(restTemplateMock.getForEntity(anyString(), eq(Map.class)))
            .thenThrow(new RuntimeException("fail-detail"));
        String res = service.getCoinDetails("bitcoin");
        assertTrue(res.contains("Error fetching details for bitcoin: fail-detail"));
    }

    // --- analyzeCoin tests ---
    @Test
    void analyzeCoin_success() throws Exception {
        // spy to override getCoinDetails
        var spy = spy(service);
        doReturn("details").when(spy).getCoinDetails("xyz");
        Map<String,Object> choice = Map.of("message", Map.of("content", "analysis-coin"));
        when(restTemplateMock.exchange(eq(OpenAIService.OPENAI_API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(new ResponseEntity<>(Map.of("choices", List.of(choice)), HttpStatus.OK));
        assertEquals("analysis-coin", spy.analyzeCoin("xyz"));
    }

    @Test
    void analyzeCoin_rateLimit() throws Exception {
        var spy = spy(service);
        doReturn("details").when(spy).getCoinDetails(anyString());
        when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
            .thenThrow(new RequestNotPermitted("rl"));
        assertEquals("Rate limit exceeded in service. Please try again later.", spy.analyzeCoin("xyz"));
    }

    @Test
    void analyzeCoin_http429() throws Exception {
        var spy = spy(service);
        doReturn("details").when(spy).getCoinDetails("xyz");
        HttpClientErrorException ex = HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS, "Too Many", null,
            "quota2".getBytes(), StandardCharsets.UTF_8);
        when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class))).thenThrow(ex);
        assertEquals("OpenAI API quota exceeded. Please check your plan and billing details.", spy.analyzeCoin("xyz"));
    }

    @Test
    void analyzeCoin_httpErrorOther() throws Exception {
        var spy = spy(service);
        doReturn("details").when(spy).getCoinDetails("xyz");
        HttpClientErrorException ex = HttpClientErrorException.create(
            HttpStatus.BAD_REQUEST, "Bad", null,
            "err2".getBytes(), StandardCharsets.UTF_8);
        when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class))).thenThrow(ex);
        assertEquals("API error: err2", spy.analyzeCoin("xyz"));
    }

    @Test
    void analyzeCoin_exception() throws Exception {
        var spy = spy(service);
        doReturn("details").when(spy).getCoinDetails("xyz");
        when(restTemplateMock.exchange(anyString(), eq(HttpMethod.POST), any(), eq(Map.class)))
            .thenThrow(new RuntimeException("fail-coin"));
        assertEquals("Error: fail-coin", spy.analyzeCoin("xyz"));
    }
}
