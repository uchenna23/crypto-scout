package com.example.crypto_scout;

import com.example.crypto_scout.Service.CryptoWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CryptoWebSocketHandlerIntegrationTest {

    private SimpMessagingTemplate messagingTemplate;
    private StringRedisTemplate redisTemplate;
    private CryptoWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        redisTemplate = mock(StringRedisTemplate.class);
        handler = new CryptoWebSocketHandler(messagingTemplate, redisTemplate);
    }

    @Test
    void testProcessTickerObject_AddsToBufferAndCachesPrice() {
        // Arrange: create a ticker object
        var ticker = new java.util.HashMap<String, Object>();
        ticker.put("product_id", "BTC-USD");
        ticker.put("price", "50000");

        // Act: call processTickerObject via reflection (since it's private)
        try {
            var method = CryptoWebSocketHandler.class.getDeclaredMethod("processTickerObject", Object.class);
            method.setAccessible(true);
            method.invoke(handler, ticker);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }

        // Assert: verify Redis cache was updated
        verify(redisTemplate).opsForValue();
    }

    @Test
    void testTrackPriceChange_TrueOnFirstInsert() throws Exception {
        var method = CryptoWebSocketHandler.class.getDeclaredMethod("trackPriceChange", String.class, double.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(handler, "BTC-USD", 50000.0);
        assertTrue(result);
    }

    @Test
    void testRateLimitPassed_TrueOnFirstCall() throws Exception {
        var method = CryptoWebSocketHandler.class.getDeclaredMethod("rateLimitPassed", String.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(handler, "BTC-USD");
        assertTrue(result);
    }

    @Test
    void testCachePriceUpdate_SetsValueInRedis() throws Exception {
        var method = CryptoWebSocketHandler.class.getDeclaredMethod("cachePriceUpdate", String.class, double.class);
        method.setAccessible(true);
        method.invoke(handler, "BTC-USD", 50000.0);
        verify(redisTemplate).opsForValue();
    }
}