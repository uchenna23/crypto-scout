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

    // Buffer of JSON strings to send to Angular (e.g. "{ \"BTC-USD\": 105293.56 }")
    private final ConcurrentLinkedQueue<String> messageBuffer = new ConcurrentLinkedQueue<>();

    // Single-thread executor to avoid concurrency issues in onMessage()
    private final ExecutorService messageProcessingExecutor = Executors.newSingleThreadExecutor();

    // Stores last known price for each product to check for significant changes
    private final Map<String, Double> lastPrices = new ConcurrentHashMap<>();

    @Value("${coinbase.api.key}")
    private String apiKey;

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    // ✅ Lower threshold to ensure we store the first message (if lastPrice < 0)
    private static final double PRICE_CHANGE_THRESHOLD = 0.1; // e.g., 0.1% changes

    public CryptoWebSocketHandler(SimpMessagingTemplate messagingTemplate, StringRedisTemplate redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
        connectToCoinbase();
        startBatchProcessor();
        startKeepAlive();
    }

    /**
     * Connects to Coinbase Advanced Trade WebSocket.
     * Subscribes to relevant ticker channels.
     */
    private void connectToCoinbase() {
        try {
            coinbaseWebSocket = new WebSocketClient(new URI("wss://advanced-trade-ws.coinbase.com")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("✅ Connected to Coinbase WebSocket");

                    // Subscribe to ticker for multiple products
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

                @Override
                public void onMessage(String message) {
                    System.out.println("📩 Received raw message from Coinbase: " + message);

                    // Process each message on a single-thread executor
                    messageProcessingExecutor.submit(() -> {
                        try {
                            Map<String, Object> data = objectMapper.readValue(
                                    message, new TypeReference<Map<String, Object>>() {}
                            );

                            // "events" is where ticker data resides
                            Object eventsObj = data.get("events");
                            if (!(eventsObj instanceof List<?>)) {
                                // Possibly "subscribe" confirmations or other messages
                                return;
                            }

                            List<?> eventsList = (List<?>) eventsObj;
                            List<Map<String, Object>> events = new ArrayList<>();

                            for (Object obj : eventsList) {
                                if (obj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> event = (Map<String, Object>) obj;
                                    events.add(event);
                                }
                            }

                            // Each "event" may contain multiple "tickers"
                            for (Map<String, Object> event : events) {
                                Object tickersObj = event.get("tickers");
                                if (!(tickersObj instanceof List<?>)) continue;

                                List<?> tickersList = (List<?>) tickersObj;
                                List<Map<String, Object>> tickers = new ArrayList<>();

                                for (Object tickerObj : tickersList) {
                                    if (tickerObj instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> ticker = (Map<String, Object>) tickerObj;
                                        tickers.add(ticker);
                                    }
                                }

                                // Extract productId and price from each ticker
                                for (Map<String, Object> ticker : tickers) {
                                    if (!ticker.containsKey("product_id") || !ticker.containsKey("price")) {
                                        continue;
                                    }

                                    String productId = (String) ticker.get("product_id");
                                    double newPrice = Double.parseDouble((String) ticker.get("price"));

                                    // Decide if we add this to the buffer
                                    if (trackPriceChange(productId, newPrice)) {
                                        cachePriceUpdate(productId, newPrice);

                                        String formattedMessage = "{ \"" + productId + "\": " + newPrice + " }";
                                        messageBuffer.add(formattedMessage);
                                        System.out.println("📥 Added to messageBuffer: " + formattedMessage);
                                        System.out.println("📊 Buffer Size: " + messageBuffer.size());
                                    }
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

    /**
     * Determines if we should buffer this price update, based on the threshold.
     * Ensures first update always triggers (lastPrice == -1.0).
     */
    private boolean trackPriceChange(String productId, double newPrice) {
        double lastPrice = lastPrices.getOrDefault(productId, -1.0);

        // If first time (lastPrice < 0), store newPrice and return true
        if (lastPrice < 0.0) {
            lastPrices.put(productId, newPrice);
            return true;
        }

        // Percentage change
        double pctChange = Math.abs((newPrice - lastPrice) / lastPrice) * 100;
        if (pctChange > PRICE_CHANGE_THRESHOLD) {
            lastPrices.put(productId, newPrice);
            return true;
        }
        return false;
    }

    /**
     * Cache the price in Redis for future reference.
     */
    private void cachePriceUpdate(String productId, double price) {
        redisTemplate.opsForValue().set("crypto:" + productId, String.valueOf(price), 1, TimeUnit.HOURS);
    }

    /**
     * Periodically sends aggregated updates to Angular via /topic/crypto.
     */
    private void startBatchProcessor() {
        // Start after 5 seconds, then run every 10 seconds
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("🕒 Batch Processor Running...");
            int bufferSize = messageBuffer.size();
            System.out.println("📊 Buffer Size Before Processing: " + bufferSize);

            if (!messageBuffer.isEmpty()) {
                try {
                    Map<String, Double> batchedUpdates = new HashMap<>();
                    while (!messageBuffer.isEmpty()) {
                        String updateStr = messageBuffer.poll();
                        if (updateStr == null) break;

                        System.out.println("🗑️ Processing message: " + updateStr);
                        Map<String, Double> update = objectMapper.readValue(
                                updateStr, new TypeReference<Map<String, Double>>() {}
                        );
                        batchedUpdates.putAll(update);
                    }

                    if (!batchedUpdates.isEmpty()) {
                        String json = objectMapper.writeValueAsString(batchedUpdates);
                        System.out.println("🔄 Preparing to send update: " + json);
                        messagingTemplate.convertAndSend("/topic/crypto", json);
                        System.out.println("📩 Sent to WebSocket Clients: " + json);
                    } else {
                        System.out.println("⚠️ No messages to send.");
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error sending update: " + e.getMessage());
                }
            } else {
                System.out.println("⚠️ Message buffer is empty.");
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    /**
     * Periodic ping to keep the WebSocket alive.
     */
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

    /**
     * Reconnect if the Coinbase WebSocket closes or errors out.
     */
    private void reconnectToCoinbase() {
        System.out.println("🔄 Reconnecting to Coinbase in 10 seconds...");
        scheduler.schedule(this::connectToCoinbase, 10, TimeUnit.SECONDS);
    }
}
