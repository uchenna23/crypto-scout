package com.example.crypto_scout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import com.example.crypto_scout.Service.CryptoPriceService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CryptoPriceServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CryptoPriceService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // inject mock RestTemplate
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    void testGetCryptoPrice_invalidPair() {
        Map<String, Object> result = service.getCryptoPrice(null);
        assertEquals(Map.of("error", "Invalid currency pair"), result);

        result = service.getCryptoPrice("");
        assertEquals(Map.of("error", "Invalid currency pair"), result);
    }

    @Test
    void testGetCryptoPrice_noDataFromCoinbase() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);
        Map<String, Object> result = service.getCryptoPrice("BTC-USD");
        assertEquals(Map.of("error", "No data returned from Coinbase API"), result);
    }

    @Test
    void testGetCryptoPrice_exception() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenThrow(new RuntimeException("fail"));
        Map<String, Object> result = service.getCryptoPrice("BTC-USD");
        assertEquals(Map.of("error", "Failed to retrieve data"), result);
    }

    @Test
    void testGetCryptoPrice_successWithChart() {
        Map<String, Object> coinbaseData = Map.of(
            "data", Map.of("base", "BTC", "currency", "USD", "amount", "123.45")
        );
        Map<String, Object> chartData = Map.of("prices", List.of(List.of(1,2), List.of(3,4)));
        when(restTemplate.getForObject(contains("prices/BTC-USD/spot"), eq(Map.class))).thenReturn(coinbaseData);
        when(restTemplate.getForObject(contains("market_chart"), eq(Map.class))).thenReturn(chartData);

        Map<String, Object> result = service.getCryptoPrice("BTC-USD");
        assertEquals("123.45", result.get("amount"));
        assertEquals(chartData.get("prices"), result.get("chart"));
    }

    @Test
    void testGetCryptoPrice_successWithoutChart() {
        Map<String, Object> coinbaseData = Map.of(
            "data", Map.of("base", "ETH", "currency", "USD", "amount", "99.99")
        );
        when(restTemplate.getForObject(contains("prices/ETH-USD/spot"), eq(Map.class))).thenReturn(coinbaseData);
        when(restTemplate.getForObject(contains("market_chart"), eq(Map.class))).thenReturn(Map.of());

        Map<String, Object> result = service.getCryptoPrice("ETH-USD");
        assertEquals("99.99", result.get("amount"));
        assertEquals("Historical data not available", result.get("chart"));
    }

    @Test
    void testGetMultipleCryptoPrices() {
        // spy fetchCryptoData
        CryptoPriceService spy = spy(service);
        doReturn(Map.of("amt", 1)).when(spy).getCryptoPrice("A-USD");
        doReturn(Map.of("amt", 2)).when(spy).getCryptoPrice("B-USD");

        Map<String, Object> result = spy.getMultipleCryptoPrices(List.of("A-USD", "B-USD"));
        assertEquals(2, result.size());
        assertEquals(Map.of("amt", 1), result.get("A-USD"));
        assertEquals(Map.of("amt", 2), result.get("B-USD"));
    }

    @Test
    void testGetCoinGeckoId_mapping() {
        // known mappings
        assertEquals("bitcoin", service.getCoinGeckoId("BTC"));
        assertEquals("ethereum", service.getCoinGeckoId("ETH"));
        assertEquals("dogecoin", service.getCoinGeckoId("DOGE"));
        // default lowercase
        assertEquals("unknown", service.getCoinGeckoId("UNKNOWN"));
    }
}
