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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class CryptoWebSocketHandler extends TextWebSocketHandler {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private WebSocketClient coinbaseWebSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentLinkedQueue<String> messageBuffer = new ConcurrentLinkedQueue<>();
    private final ExecutorService messageProcessingExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, Double> lastPrices = new ConcurrentHashMap<>();

    @Value("${coinbase.api.key}")
    private String apiKey;

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    private static final double PRICE_CHANGE_THRESHOLD = 5.0; // Only update if price changes by 5%
    private static final int UPDATE_INTERVAL = 60; // Send updates every 60 seconds

    public CryptoWebSocketHandler(SimpMessagingTemplate messagingTemplate, StringRedisTemplate redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
        connectToCoinbase();
        startBatchProcessor();
        startKeepAlive();
    }

    private void connectToCoinbase() {
        try {
            coinbaseWebSocket = new WebSocketClient(new URI("wss://advanced-trade-ws.coinbase.com")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("✅ Connected to Coinbase WebSocket");

                    Map<String, Object> subscribeMessage = Map.of(
                            "type", "subscribe",
                            "channel", "ticker",
                            "product_ids", List.of("BTC-USD", "ETH-USD", "USDT-USD", "BNB-USD", "SOL-USD")
                    );

                    try {
                        String jsonMessage = objectMapper.writeValueAsString(subscribeMessage);
                        send(jsonMessage);
                        System.out.println("📩 Sent subscription request.");
                    } catch (Exception e) {
                        System.err.println("❌ Error creating subscription message: " + e.getMessage());
                    }

                }

                @SuppressWarnings("unchecked")
                @Override
                public void onMessage(String message) {
                    System.out.println("📩 Received raw message from Coinbase: " + message);

                    messageProcessingExecutor.submit(() -> {
                        try {
                            Map<String, Object> data = objectMapper.readValue(message, Map.class);
                            if (!data.containsKey("events")) return;

                            List<Map<String, Object>> events = (List<Map<String, Object>>) data.get("events");
                            for (Map<String, Object> event : events) {
                                if (!event.containsKey("tickers")) continue;

                                List<Map<String, Object>> tickers = (List<Map<String, Object>>) event.get("tickers");
                                for (Map<String, Object> ticker : tickers) {
                                    if (!ticker.containsKey("product_id") || !ticker.containsKey("price")) continue;

                                    String productId = (String) ticker.get("product_id");
                                    double newPrice = Double.parseDouble((String) ticker.get("price"));

                                    if (trackPriceChange(productId, newPrice)) {
                                        cachePriceUpdate(productId, newPrice);
                                        messageBuffer.add("{\"" + productId + "\": " + newPrice + "}");
                                        System.out.println("📈 Processed update: " + productId + " -> " + newPrice);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("❌ Error processing message: " + e.getMessage());
                        }
                    });
                }



                private boolean trackPriceChange(String productId, double newPrice) {
                    double lastPrice = lastPrices.getOrDefault(productId, 0.0);
                    if (Math.abs((newPrice - lastPrice) / lastPrice * 100) > PRICE_CHANGE_THRESHOLD) {
                        lastPrices.put(productId, newPrice);
                        return true;
                    }
                    return false;
                }

                private void cachePriceUpdate(String productId, double price) {
                    redisTemplate.opsForValue().set("crypto:" + productId, String.valueOf(price), 1, TimeUnit.HOURS);
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
                        System.out.println("📩 Sent to WebSocket Clients: " + json); // 🔍 Debugging log
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error sending update: " + e.getMessage());
                }
            }
        }, 0, UPDATE_INTERVAL, TimeUnit.SECONDS);
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
        scheduler.schedule(this::connectToCoinbase, 10, TimeUnit.SECONDS);
        
    }
}
