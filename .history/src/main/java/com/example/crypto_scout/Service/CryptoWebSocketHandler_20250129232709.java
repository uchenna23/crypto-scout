package com.example.crypto_scout.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.util.Base64;
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
    private final ObjectMapper objectMapper = new ObjectMapper(); // Use Jackson for JSON

    // 🔹 Securely load API credentials from application.properties
    @Value("${coinbase.api.key}")
    private String apiKey;

    @Value("${coinbase.api.secret}")
    private String apiSecret;

    public CryptoWebSocketHandler() {
        connectToCoinbase();
    }

    private void connectToCoinbase() {
        try {
            coinbaseWebSocket = new WebSocketClient(new URI("wss://advanced-trade-ws.coinbase.com")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("✅ Connected to Coinbase WebSocket");

                    long timestamp = System.currentTimeMillis() / 1000;
                    String signature = generateSignature(timestamp, apiSecret);

                    // 🔹 Authentication Payload
                    Map<String, Object> authMessage = Map.of(
                            "type", "authenticate",
                            "api_key", apiKey,
                            "timestamp", timestamp,
                            "signature", signature
                    );

                    try {
                        String jsonMessage = objectMapper.writeValueAsString(authMessage);
                        send(jsonMessage);
                    } catch (Exception e) {
                        System.err.println("❌ Error creating authentication message: " + e.getMessage());
                    }
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("📩 Received: " + message);
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

    // 🔹 Generates the required authentication signature
    private String generateSignature(long timestamp, String apiSecret) {
        try {
            String message = timestamp + "GET" + "/users/self/verify";
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256");
            sha256Hmac.init(secretKeySpec);
            return Base64.getEncoder().encodeToString(sha256Hmac.doFinal(message.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Error generating Coinbase authentication signature", e);
        }
    }

    private void reconnectToCoinbase() {
        System.out.println("🔄 Attempting to reconnect to Coinbase WebSocket...");
        scheduler.schedule(this::connectToCoinbase, 5, TimeUnit.SECONDS); // Reconnect after 5 seconds
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("✅ Client Connected to WebSocket");

        // Start a background thread to send crypto price updates to connected clients
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (coinbaseWebSocket != null && coinbaseWebSocket.isOpen()) {
                    String latestData = getLatestCryptoData();
                    session.sendMessage(new TextMessage(latestData));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 3, TimeUnit.SECONDS); // Update every 3 seconds
    }

    private String getLatestCryptoData() {
        // Simulate getting the latest crypto price data (replace with real API integration)
        return "{\"BTC-USD\": \"43012.25\", \"ETH-USD\": \"3123.55\"}";
    }
}
