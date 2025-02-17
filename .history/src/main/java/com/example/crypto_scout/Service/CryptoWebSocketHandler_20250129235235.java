package com.example.crypto_scout.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${coinbase.api.key}")
    private String apiKey;

    private WebSocketSession clientSession; // Store connected client session
    private int reconnectAttempts = 0;

    public CryptoWebSocketHandler() {
        connectToCoinbase();
        startBatchProcessor();
    }

    private void connectToCoinbase() {
        try {
            coinbaseWebSocket = new WebSocketClient(new URI("wss://advanced-trade-ws.coinbase.com")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("✅ Connected to Coinbase WebSocket");

                    // 🔹 Subscribe to the top 5 most popular cryptocurrencies
                    Map<String, Object> subscribeMessage = Map.of(
                            "type", "subscribe",
                            "channel", "ticker",
                            "product_ids", List.of("BTC-USD", "ETH-USD", "USDT-USD", "BNB-USD", "SOL-USD")
                    );

                    try {
                        String jsonMessage = objectMapper.writeValueAsString(subscribeMessage);
                        send(jsonMessage);
                        System.out.println("📩 Sent subscription request: " + jsonMessage);
                    } catch (Exception e) {
                        System.err.println("❌ Error creating subscription message: " + e.getMessage());
                    }

                    reconnectAttempts = 0; // Reset reconnect attempts after successful connection
                }

                private final Map<String, Double> lastPrices = new ConcurrentHashMap<>();
                private final double PRICE_CHANGE_THRESHOLD = 0.5; // Ignore price changes <0.5%

                @Override
                public void onMessage(String message) {
                    messageProcessingExecutor.submit(() -> {
                        try {
                            Map<String, Object> data = objectMapper.readValue(message, Map.class);

                            if (!data.containsKey("type") || !data.get("type").equals("ticker")) {
                                return; // Ignore non-ticker messages
                            }

                            String productId = (String) data.get("product_id");
                            if (!data.containsKey("price")) {
                                return; // Ignore invalid messages
                            }

                            double newPrice = Double.parseDouble((String) data.get("price"));

                            // Track price changes for the top 5 cryptos
                            if (trackPriceChange(productId, newPrice)) {
                                messageBuffer.add("{\"" + productId + "\": " + newPrice + "}");
                                System.out.println("📈 " + productId + " Price Update: " + newPrice);
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
                        return true; // Only process if price change is significant
                    }
                    return false; // Ignore minor changes
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

    // 🔹 Scheduled task to process messages in batches and send to WebSocket clients
    private void startBatchProcessor() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!messageBuffer.isEmpty() && clientSession != null && clientSession.isOpen()) {
                try {
                    for (String msg : messageBuffer) {
                        clientSession.sendMessage(new TextMessage(msg));
                    }
                    System.out.println("📩 Sent batch of " + messageBuffer.size() + " messages.");
                    messageBuffer.clear();
                } catch (Exception e) {
                    System.err.println("❌ Error sending batch messages: " + e.getMessage());
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
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
