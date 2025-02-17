package com.example.crypto_scout.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

@Service
public class CryptoWebSocketHandler extends TextWebSocketHandler {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private WebSocketClient coinbaseWebSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentLinkedQueue<String> messageBuffer = new ConcurrentLinkedQueue<>();
    private final ExecutorService messageProcessingExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, Double> lastPrices = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUpdateTimestamps = new ConcurrentHashMap<>();

    @Value("${coinbase.api.key}")
    private String apiKey;

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    private static final double PRICE_CHANGE_THRESHOLD = 0.1;
    private static final long MIN_UPDATE_INTERVAL_MS = 2000;

    public CryptoWebSocketHandler(SimpMessagingTemplate messagingTemplate, StringRedisTemplate redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;

        connectToCoinbase();
        startBatchProcessor();
        startKeepAlive();
    }

    /**
     * Connect to Coinbase's Advanced Trade WebSocket.
     */
    private void connectToCoinbase() {
        try {
            coinbaseWebSocket = new WebSocketClient(new URI("wss://advanced-trade-ws.coinbase.com")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("✅ Connected to Coinbase WebSocket");

                    // ✅ SUBSCRIBE TO BTC, ETH, ADA, SOL, XRP
                    Map<String, Object> subscribeMessage = Map.of(
                            "type", "subscribe",
                            "channel", "ticker",
                            "product_ids", List.of("BTC-USD", "ETH-USD", "ADA-USD", "SOL-USD", "XRP-USD")
                    );

                    try {
                        String jsonMessage = objectMapper.writeValueAsString(subscribeMessage);
                        send(jsonMessage);
                        System.out.println("📩 Sent subscription request.");
                    } catch (Exception e) {
                        System.err.println("❌ Error creating subscription message: " + e.getMessage());
                    }
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("📩 Received raw message from Coinbase: " + message);

                    messageProcessingExecutor.submit(() -> {
                        try {
                            Map<String, Object> data = objectMapper.readValue(message, new TypeReference<>() {});
                            Object eventsObj = data.get("events");
                            if (!(eventsObj instanceof List<?>)) {
                                return;
                            }

                            List<?> eventsList = (List<?>) eventsObj;
                            for (Object obj : eventsList) {
                                if (!(obj instanceof Map)) continue;
                                @SuppressWarnings("unchecked")
                                Map<String, Object> event = (Map<String, Object>) obj;

                                Object tickersObj = event.get("tickers");
                                if (!(tickersObj instanceof List<?>)) continue;
                                List<?> tickersList = (List<?>) tickersObj;

                                for (Object tObj : tickersList) {
                                    if (!(tObj instanceof Map)) continue;
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> ticker = (Map<String, Object>) tObj;

                                    if (!ticker.containsKey("product_id") || !ticker.containsKey("price")) {
                                        continue;
                                    }

                                    String productId = (String) ticker.get("product_id");
                                    double newPrice = Double.parseDouble((String) ticker.get("price"));

                                    if (!trackPriceChange(productId, newPrice)) {
                                        continue;
                                    }

                                    if (!rateLimitPassed(productId)) {
                                        continue;
                                    }

                                    cachePriceUpdate(productId, newPrice);
                                    String formattedMessage = "{ \"" + productId + "\": " + newPrice + " }";
                                    messageBuffer.add(formattedMessage);
                                    System.out.println("📥 Added to messageBuffer: " + formattedMessage);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("❌ Error processing message: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("⚠️ Coinbase WebSocket closed: " + reason);
                    reconnectToCoinbase();
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("🚨 WebSocket Error: " + ex.getMessage());
                }
            };
            coinbaseWebSocket.connect();
        } catch (Exception e) {
            System.err.println("🚨 WebSocket connection failed: " + e.getMessage());
            reconnectToCoinbase();
        }
    }

    private boolean trackPriceChange(String productId, double newPrice) {
        double lastPrice = lastPrices.getOrDefault(productId, -1.0);

        if (lastPrice < 0.0) {
            lastPrices.put(productId, newPrice);
            return true;
        }

        double pctChange = Math.abs((newPrice - lastPrice) / lastPrice) * 100.0;
        if (pctChange > PRICE_CHANGE_THRESHOLD) {
            lastPrices.put(productId, newPrice);
            return true;
        }
        return false;
    }

    private boolean rateLimitPassed(String productId) {
        long now = System.currentTimeMillis();
        long lastTime = lastUpdateTimestamps.getOrDefault(productId, 0L);

        if ((now - lastTime) > MIN_UPDATE_INTERVAL_MS) {
            lastUpdateTimestamps.put(productId, now);
            return true;
        }
        return false;
    }

    private void cachePriceUpdate(String productId, double price) {
        redisTemplate.opsForValue().set("crypto:" + productId, String.valueOf(price), 1, TimeUnit.HOURS);
    }

    private void startBatchProcessor() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!messageBuffer.isEmpty()) {
                try {
                    Map<String, Double> batchedUpdates = new HashMap<>();
                    while (!messageBuffer.isEmpty()) {
                        String updateStr = messageBuffer.poll();
                        if (updateStr == null) break;

                        Map<String, Double> update = objectMapper.readValue(updateStr, new TypeReference<>() {});
                        batchedUpdates.putAll(update);
                    }

                    if (!batchedUpdates.isEmpty()) {
                        String json = objectMapper.writeValueAsString(batchedUpdates);
                        messagingTemplate.convertAndSend("/topic/crypto", json);
                        System.out.println("📩 Sent to WebSocket Clients: " + json);
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error sending update: " + e.getMessage());
                }
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    private void startKeepAlive() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (coinbaseWebSocket != null && coinbaseWebSocket.isOpen()) {
                    coinbaseWebSocket.send("{\"type\":\"ping\"}");
                    System.out.println("🔄 Sent keep-alive ping");
                }
            } catch (Exception e) {
                System.err.println("❌ Keep-alive ping failed: " + e.getMessage());
            }
        }, 10, 10, TimeUnit.MINUTES);
    }

    private void reconnectToCoinbase() {
        System.out.println("🔄 Reconnecting to Coinbase in 10 seconds...");
        scheduler.schedule(this::connectToCoinbase, 10, TimeUnit.SECONDS);
    }
}
