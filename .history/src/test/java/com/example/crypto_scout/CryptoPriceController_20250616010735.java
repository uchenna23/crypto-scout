package com.example.crypto_scout;

import com.example.crypto_scout.Service.CryptoPriceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import com.example.crypto_scout.Controller.CryptoPriceController;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoPriceControllerTest {

    @Mock
    private CryptoPriceService cryptoPriceService;

    @InjectMocks
    private CryptoPriceController controller;

    @Test
    void getCryptoPrice_successReturnsOk() {
        // Arrange
        String pair = "BTC-USD";
        Map<String, Object> serviceResponse = Map.of("price", 45000.0);
        when(cryptoPriceService.getCryptoPrice(pair)).thenReturn(serviceResponse);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.getCryptoPrice(pair);

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(serviceResponse, response.getBody());
        verify(cryptoPriceService).getCryptoPrice(pair);
    }

    @Test
    void getCryptoPrice_errorReturnsBadRequest() {
        // Arrange
        String pair = "UNKNOWN";
        Map<String, Object> serviceResponse = Map.of("error", "Not found");
        when(cryptoPriceService.getCryptoPrice(pair)).thenReturn(serviceResponse);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.getCryptoPrice(pair);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        assertEquals(serviceResponse, response.getBody());
        verify(cryptoPriceService).getCryptoPrice(pair);
    }

    @Test
    void getBulkPrices_invalidPairReturnsBadRequest() {
        // Arrange
        List<String> invalidList = List.of("BTC/USD", "ETH-USD");

        // Act
        ResponseEntity<Map<String, Object>> response = controller.getBulkPrices(invalidList);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("error"));
        assertEquals("Invalid currency pair: BTC/USD", body.get("error"));
        verifyNoInteractions(cryptoPriceService);
    }

    @Test
    void getBulkPrices_emptyServiceResponseReturnsBadRequest() {
        // Arrange
        List<String> pairs = List.of("BTC-USD", "ETH-USD");
        when(cryptoPriceService.getMultipleCryptoPrices(pairs)).thenReturn(Map.of());

        // Act
        ResponseEntity<Map<String, Object>> response = controller.getBulkPrices(pairs);

        // Assert
        assertEquals(400, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("No valid data returned", body.get("error"));
        verify(cryptoPriceService).getMultipleCryptoPrices(pairs);
    }

    @Test
    void getBulkPrices_successReturnsOk() {
        // Arrange
        List<String> pairs = List.of("BTC-USD", "ETH-USD");
        Map<String, Object> serviceResponse = Map.of("BTC-USD", 45000.0, "ETH-USD", 3000.0);
        when(cryptoPriceService.getMultipleCryptoPrices(pairs)).thenReturn(serviceResponse);

        // Act
        ResponseEntity<Map<String, Object>> response = controller.getBulkPrices(pairs);

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(serviceResponse, response.getBody());
        verify(cryptoPriceService).getMultipleCryptoPrices(pairs);
    }
}
