package com.example.crypto_scout.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

@Service
public class CryptoWebSocketHandler extends TextWebSocketHandler {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private WebSocketClient coinbaseWebSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> messageBuffer = new ArrayList<>();

    @Value("${coinbase.api.key}")
    private String apiKey;

    private WebSocketSession clientSession; // Store connected client session

    public CryptoWebSocketHandler() {
        connectToCoinbase();
        startBatchProcessor(); // 🔹 Start batch processing when service initializes
    }

    private void connectToCoinbase() {
        try {
            coinbaseWebSocket = new WebSocketClient(new URI("wss://advanced-trade-ws.coinbase.com")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("✅ Connected to Coinbase WebSocket");

                    // 🔹 Subscribe to BTC-USD & ETH-USD using "ticker" (not market_trades)
                    Map<String, Object> subscribeMessage = Map.of(
                            "type", "subscribe",
                            "channel", "ticker",
                            "product_ids", List.of("BTC-USD", "ETH-USD")
                    );

                    try {
                        String jsonMessage = objectMapper.writeValueAsString(subscribeMessage);
                        send(jsonMessage);
                        System.out.println("📩 Sent subscription request: " + jsonMessage);
                    } catch (Exception e) {
                        System.err.println("❌ Error creating subscription message: " + e.getMessage());
                    }
                }

                @Override
                public void onMessage(String message) {
                    synchronized (messageBuffer) {
                        messageBuffer.add(message);
                    }
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
        }
    }

    // 🔹 Scheduled task to process messages in batches and send to WebSocket clients
    private void startBatchProcessor() {
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (messageBuffer) {
                if (!messageBuffer.isEmpty()) {
                    System.out.println("📩 Processing batch of " + messageBuffer.size() + " messages.");
                    
                    // 🔹 Send messages to connected WebSocket clients
                    if (clientSession != null && clientSession.isOpen()) {
                        try {
                            for (String msg : messageBuffer) {
                                clientSession.sendMessage(new TextMessage(msg));
                            }
                        } catch (Exception e) {
                            System.err.println("❌ Error sending batch messages: " + e.getMessage());
                        }
                    }
                    
                    messageBuffer.clear(); // Clear buffer after processing
                }
            }
        }, 0, 5, TimeUnit.SECONDS); // 🔹 Process every 5 seconds
    }

    private void reconnectToCoinbase() {
        System.out.println("🔄 Attempting to reconnect to Coinbase WebSocket...");
        scheduler.schedule(this::connectToCoinbase, 5, TimeUnit.SECONDS); // Reconnect after 5 seconds
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("✅ Client Connected to WebSocket");
        this.clientSession = session; // 🔹 Store client session for sending updates
    }
}
