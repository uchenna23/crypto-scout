package com.example.crypto_scout;

import com.example.crypto_scout.Service.OpenAIService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import com.example.crypto_scout.Controller.OpenAIController;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenAIControllerTest {

    @Mock
    private OpenAIService openAIService;

    @InjectMocks
    private OpenAIController controller;

    @Test
    void chatWithBot_analysisRequest_callsAnalyzeCoin() {
        // Arrange
        Map<String, String> request = Map.of("query", "Please provide an analysis of btc market.");
        when(openAIService.analyzeCoin("bitcoin")).thenReturn("analysis-result");

        // Act
        ResponseEntity<String> response = controller.chatWithBot(request);

        // Assert
        verify(openAIService).analyzeCoin("bitcoin");
        assertEquals("analysis-result", response.getBody());
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    void chatWithBot_detailsRequest_callsGetCoinDetails() {
        // Arrange
        Map<String, String> request = Map.of("query", "What is the price of ETH today?");
        when(openAIService.getCoinDetails("ethereum")).thenReturn("details-result");

        // Act
        ResponseEntity<String> response = controller.chatWithBot(request);

        // Assert
        verify(openAIService).getCoinDetails("ethereum");
        assertEquals("details-result", response.getBody());
    }

    @Test
    void chatWithBot_coinAliasWithoutKeywords_callsAnalyzeMarketTrends() {
        // Arrange
        Map<String, String> request = Map.of("query", "btc info");
        when(openAIService.analyzeMarketTrends("btc info")).thenReturn("general-analysis");

        // Act
        ResponseEntity<String> response = controller.chatWithBot(request);

        // Assert
        verify(openAIService).analyzeMarketTrends("btc info");
        assertEquals("general-analysis", response.getBody());
    }

    @Test
    void chatWithBot_noAlias_callsAnalyzeMarketTrends() {
        // Arrange
        Map<String, String> request = Map.of("query", "hello world");
        when(openAIService.analyzeMarketTrends("hello world")).thenReturn("greeting-response");

        // Act
        ResponseEntity<String> response = controller.chatWithBot(request);

        // Assert
        verify(openAIService).analyzeMarketTrends("hello world");
        assertEquals("greeting-response", response.getBody());
    }

    @Test
    void chatWithBot_usesCryptoDataWhenQueryMissing() {
        // Arrange
        Map<String, String> request = Map.of("cryptoData", "Please analyze LTC trends.");
        when(openAIService.analyzeCoin("litecoin")).thenReturn("ltc-analysis");

        // Act
        ResponseEntity<String> response = controller.chatWithBot(request);

        // Assert
        verify(openAIService).analyzeCoin("litecoin");
        assertEquals("ltc-analysis", response.getBody());
    }
}
