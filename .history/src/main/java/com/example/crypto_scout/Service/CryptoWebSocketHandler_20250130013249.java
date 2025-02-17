package com.example.crypto_scout.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

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

    private WebSocketSession clientSession; // Store connected client session
    private int reconnectAttempts = 0;

    private static final double PRICE_CHANGE_THRESHOLD = 1.0; // ✅ Only update if price changes by 1%
    private static final int UPDATE_INTERVAL = 10; // ✅ Send updates every 10 seconds

    private final StringRedisTemplate redisTemplate;

    public CryptoWebSocketHandler(StringRedisTemplate redisTemplate) {
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

                    reconnectAttempts = 0; // Reset reconnect attempts after successful connection
                }

                @Override
                public void onMessage(String message) {
                    messageProcessingExecutor.submit(() -> {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> data = objectMapper.readValue(message, Map.class);

                            if (!data.containsKey("type") || !data.get("type").equals("ticker")) {
                                return; // Ignore non-ticker messages
                            }

                            String productId = (String) data.get("product_id");
                            if (!data.containsKey("price")) {
                                return; // Ignore invalid messages
                            }

                            double newPrice = Double.parseDouble((String) data.get("price"));

                            // ✅ Only update if price changes by more than 1%
                            if (trackPriceChange(productId, newPrice)) {
                                cachePriceUpdate(productId, newPrice);
                                messageBuffer.add("{\"" + productId + "\": " + newPrice + "}");
                                System.out.println("📈 " + productId + " Significant Price Update: " + newPrice);
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
                        return true; // ✅ Only process if change > threshold
                    }
                    return false; // Ignore minor changes
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
            if (!messageBuffer.isEmpty() && clientSession != null && clientSession.isOpen()) {
                try {
                    String latestUpdate = messageBuffer.poll(); // ✅ Get only the latest update
                    if (latestUpdate != null) {
                        clientSession.sendMessage(new TextMessage(latestUpdate));
                        System.out.println("📩 Sent throttled update: " + latestUpdate);
                    }
                    messageBuffer.clear();
                } catch (Exception e) {
                    System.err.println("❌ Error sending batch messages: " + e.getMessage());
                }
            }
        }, 0, UPDATE_INTERVAL, TimeUnit.SECONDS); // ✅ Now updates every 10 seconds
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
        }, 10, 10, TimeUnit.MINUTES); // ✅ Sends a ping every 10 minutes
    }

    private void reconnectToCoinbase() {
        int delay = (int) Math.min(60, Math.pow(2, reconnectAttempts)) * 1000; // Exponential backoff, max 60s
        System.out.println("🔄 Attempting to reconnect in " + (delay / 1000) + " seconds...");
        
        scheduler.schedule(this::connectToCoinbase, delay, TimeUnit.MILLISECONDS);
        reconnectAttempts++;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("✅ Client Connected to WebSocket");
        this.clientSession = session;
    }
}
