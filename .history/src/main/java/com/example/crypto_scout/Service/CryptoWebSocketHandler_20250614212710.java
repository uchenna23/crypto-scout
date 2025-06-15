package com.example.crypto_scout.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

@Service
public class CryptoWebSocketHandler extends TextWebSocketHandler {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CryptoWebSocketHandler.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private WebSocketClient coinbaseWebSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentLinkedQueue<String> messageBuffer = new ConcurrentLinkedQueue<>();
    private final ExecutorService messageProcessingExecutor = Executors.newSingleThreadExecutor();
    private final Map<String, Double> lastPrices = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUpdateTimestamps = new ConcurrentHashMap<>();

    @Value("${coinbase.api.key}")
    private String apiKey;

    @Value("${coinbase.websocket.uri:wss://advanced-trade-ws.coinbase.com}")
    private String coinbaseWebSocketUri;

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    // Price tracking settings
    private static final double PRICE_CHANGE_THRESHOLD = 0.1;
    private static final long MIN_UPDATE_INTERVAL_MS = 2000;

    // Added for connection lost timeout functionality
    private int connectionLostTimeoutSeconds = 60;
    private volatile long lastPongTime = System.currentTimeMillis();

    public CryptoWebSocketHandler(SimpMessagingTemplate messagingTemplate, StringRedisTemplate redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Initialize WebSocket connection and schedulers after properties are set.
     */
    @PostConstruct
    private void init() {
        connectToCoinbase();
        startBatchProcessor();
        startKeepAlive();
        startPongMonitor();
    }

    private void connectToCoinbase() {
        try {
            coinbaseWebSocket = new WebSocketClient(new URI(coinbaseWebSocketUri)) {

                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    logger.info("✅ Connected to Coinbase WebSocket");
                    lastPongTime = System.currentTimeMillis();
                    sendSubscriptionRequest(this);
                }

                @Override
                public void onMessage(String message) {
                    logger.info("📩 Received raw message from Coinbase: {}", message);
                    messageProcessingExecutor.submit(() -> processIncomingMessage(message));
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.info("⚠️ Coinbase WebSocket closed: {}", reason);
                    reconnectToCoinbase();
                }

                @Override
                public void onError(Exception ex) {
                    logger.error("🚨 WebSocket Error: {}", ex.getMessage());
                }

                @Override
                public void onWebsocketPong(org.java_websocket.WebSocket conn, Framedata f) {
                    lastPongTime = System.currentTimeMillis();
                    logger.info("🔄 Received pong");
                }
            };
            coinbaseWebSocket.connect();
        } catch (Exception e) {
            logger.error("🚨 WebSocket connection failed: {}", e.getMessage());
            reconnectToCoinbase();
        }
    }

    private void sendSubscriptionRequest(WebSocketClient client) {
        Map<String, Object> subscribeMessage = Map.of(
            "type", "subscribe",
            "channel", "ticker",
            "product_ids", List.of("BTC-USD", "ETH-USD", "ADA-USD", "SOL-USD", "XRP-USD")
        );
        try {
            String jsonMessage = objectMapper.writeValueAsString(subscribeMessage);
            client.send(jsonMessage);
            logger.info("📩 Sent subscription request.");
        } catch (Exception e) {
            logger.error("❌ Error creating subscription message: {}", e.getMessage());
        }
    }

    private void processIncomingMessage(String message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, new TypeReference<>() {});
            Object eventsObj = data.get("events");
            if (!(eventsObj instanceof List<?>)) return;

            for (Object obj : (List<?>) eventsObj) {
                processEventObject(obj);
            }
        } catch (Exception e) {
            logger.error("❌ Error processing message: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void processEventObject(Object obj) {
        if (!(obj instanceof Map<?, ?>)) return;
        Map<String, Object> event = (Map<String, Object>) obj;
        Object tickersObj = event.get("tickers");
        if (!(tickersObj instanceof List<?>)) return;

        for (Object tObj : (List<?>) tickersObj) {
            processTickerObject(tObj);
        }
    }

    @SuppressWarnings("unchecked")
    private void processTickerObject(Object tObj) {
        if (!(tObj instanceof Map<?, ?>)) return;
        Map<String, Object> ticker = (Map<String, Object>) tObj;
        if (!ticker.containsKey("product_id") || !ticker.containsKey("price")) return;

        String productId = (String) ticker.get("product_id");
        double newPrice = Double.parseDouble((String) ticker.get("price"));
        if (!trackPriceChange(productId, newPrice) || !rateLimitPassed(productId)) return;

        cachePriceUpdate(productId, newPrice);
        String formattedMessage = String.format("{ \"%s\": %s }", productId, newPrice);
        messageBuffer.add(formattedMessage);
        logger.info("📥 Added to messageBuffer: {}", formattedMessage);
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
            if (messageBuffer.isEmpty()) return;
            try {
                Map<String, Double> batchedUpdates = new HashMap<>();
                String updateStr;
                while ((updateStr = messageBuffer.poll()) != null) {
                    Map<String, Double> update = objectMapper.readValue(updateStr, new TypeReference<>() {});
                    batchedUpdates.putAll(update);
                }
                if (!batchedUpdates.isEmpty()) {
                    String json = objectMapper.writeValueAsString(batchedUpdates);
                    messagingTemplate.convertAndSend("/topic/crypto", json);
                    logger.info("📩 Sent to WebSocket Clients: {}", json);
                }
            } catch (Exception e) {
                logger.error("❌ Error sending update: {}", e.getMessage());
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    private void startKeepAlive() {
        scheduler.scheduleAtFixedRate(() -> {
            if (coinbaseWebSocket != null && coinbaseWebSocket.isOpen()) {
                try {
                    coinbaseWebSocket.send("{\"type\":\"ping\"}");
                    logger.info("🔄 Sent keep-alive ping");
                } catch (Exception e) {
                    logger.error("❌ Keep-alive ping failed: {}", e.getMessage());
                }
            }
        }, 5, 30, TimeUnit.SECONDS);
    }

    private void startPongMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            if (coinbaseWebSocket != null && coinbaseWebSocket.isOpen()) {
                long elapsed = System.currentTimeMillis() - lastPongTime;
                if (elapsed > connectionLostTimeoutSeconds * 1000L) {
                    logger.error("❌ No pong received within timeout ({}s). Reconnecting...", connectionLostTimeoutSeconds);
                    coinbaseWebSocket.close();
                }
            }
        }, connectionLostTimeoutSeconds, connectionLostTimeoutSeconds, TimeUnit.SECONDS);
    }

    private void reconnectToCoinbase() {
        logger.info("🔄 Reconnecting to Coinbase in 60 seconds...");
        scheduler.schedule(this::connectToCoinbase, 60, TimeUnit.SECONDS);
    }
}
