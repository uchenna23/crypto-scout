package com.example.crypto_scout;

import com.example.crypto_scout.Service.CryptoWebSocketHandler;
import com.example.crypto_scout.Service.CryptoWebSocketHandler.CryptoWebSocketInitException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CryptoWebSocketHandlerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private CryptoWebSocketHandler handler;

    // Immediate executor to run scheduled tasks immediately
    static class ImmediateScheduledExecutorService implements ScheduledExecutorService {
        private static class ImmediateScheduledFuture<V> implements ScheduledFuture<V> {
            private final V value;
            ImmediateScheduledFuture(V value) { this.value = value; }
            @Override public long getDelay(TimeUnit unit) { return 0; }
            @Override public int compareTo(Delayed o) { return 0; }
            @Override public boolean cancel(boolean mayInterruptIfRunning) { return false; }
            @Override public boolean isCancelled() { return false; }
            @Override public boolean isDone() { return true; }
            @Override public V get() { return value; }
            @Override public V get(long timeout, TimeUnit unit) { return value; }
        }
        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            command.run();
            return new ImmediateScheduledFuture<>(null);
        }
        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            command.run();
            return new ImmediateScheduledFuture<>(null);
        }
        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            command.run();
            return new ImmediateScheduledFuture<>(null);
        }
        @Override
        public void execute(Runnable command) { command.run(); }
        @Override
        public Future<?> submit(Runnable command) { command.run(); return CompletableFuture.completedFuture(null); }
        @Override
        public <T> Future<T> submit(Runnable command, T result) { command.run(); return CompletableFuture.completedFuture(result); }
        @Override
        public <T> Future<T> submit(Callable<T> task) {
            try {
                return CompletableFuture.completedFuture(task.call());
            } catch (Exception e) {
                CompletableFuture<T> future = new CompletableFuture<>();
                future.completeExceptionally(e);
                return future;
            }
        }
        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) { throw new UnsupportedOperationException(); }
        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) { throw new UnsupportedOperationException(); }
        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override
        public boolean isShutdown() { return false; }
        @Override
        public boolean isTerminated() { return false; }
        @Override
        public void shutdown() {
             //No-op
        }
        @Override
        public List<Runnable> shutdownNow() { return Collections.emptyList(); }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        handler = new CryptoWebSocketHandler(messagingTemplate, redisTemplate);
        // override scheduler
        ReflectionTestUtils.setField(handler, "scheduler", new ImmediateScheduledExecutorService());
        // set dummy URI to avoid real connections
        ReflectionTestUtils.setField(handler, "coinbaseWebSocketUri", "wss://echo.websocket.events");
        ReflectionTestUtils.setField(handler, "apiKey", "dummy");
    }

    @Test
    void testInitThrowsException() {
        // Force a null URI so new URI(null) blows up inside init()
        ReflectionTestUtils.setField(handler, "coinbaseWebSocketUri", null);

        // Call init() directly, not via invokeMethod
        assertThrows(CryptoWebSocketInitException.class, () -> handler.init());
    }

    @Test
    void testSendSubscriptionRequest() throws Exception {
        List<String> sent = new ArrayList<>();
        WebSocketClient client = new WebSocketClient(new URI("ws://dummy")) {
            @Override public void onOpen(ServerHandshake s) {
                 //No-op
            }
            @Override public void onMessage(String m) {
                 //No-op
            }
            @Override public void onClose(int c, String r, boolean rem) {
                 //No-op
            }
            @Override public void onError(Exception ex) {
                 //No-op
            }
            @Override public void send(String text) { sent.add(text); }
        };
        Method method = CryptoWebSocketHandler.class.getDeclaredMethod("sendSubscriptionRequest", WebSocketClient.class);
        method.setAccessible(true);
        method.invoke(handler, client);
        assertEquals(1, sent.size());
        String json = sent.get(0);
        assertTrue(json.contains("\"type\":\"subscribe\""));
        assertTrue(json.contains("\"channel\":\"ticker\""));
    }

    @Test
    void testProcessIncomingMessagePaths() throws Exception {
        Method method = CryptoWebSocketHandler.class.getDeclaredMethod("processIncomingMessage", String.class);
        method.setAccessible(true);
        // invalid JSON
        assertDoesNotThrow(() -> method.invoke(handler, "not-json"));
        // no events
        assertDoesNotThrow(() -> method.invoke(handler, "{}"));
        // events not list
        assertDoesNotThrow(() -> method.invoke(handler, "{\"events\":123}"));
    }

    @Test
    void testProcessIncomingMessageHappyPath() throws Exception {
        String msg = "{\"events\":[{\"tickers\":[{\"product_id\":\"BTC-USD\",\"price\":\"100.5\"}]}]}";
        // clear state
        @SuppressWarnings("unchecked")
        Map<String, Double> lastPrices = (Map<String, Double>) ReflectionTestUtils.getField(handler, "lastPrices");
        lastPrices.clear();
        @SuppressWarnings("unchecked")
        Map<String, Long> lastUpdateTimestamps = (Map<String, Long>) ReflectionTestUtils.getField(handler, "lastUpdateTimestamps");
        lastUpdateTimestamps.clear();
        Method method = CryptoWebSocketHandler.class.getDeclaredMethod("processIncomingMessage", String.class);
        method.setAccessible(true);
        method.invoke(handler, msg);
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<String> buf = (ConcurrentLinkedQueue<String>) ReflectionTestUtils.getField(handler, "messageBuffer");
        assertFalse(buf.isEmpty());
        assertEquals("{ \"BTC-USD\": 100.5 }", buf.poll());
    }

    @Test
    void testTrackPriceChangeRateLimit() throws Exception {
        Method track = CryptoWebSocketHandler.class.getDeclaredMethod("trackPriceChange", String.class, double.class);
        track.setAccessible(true);
        Method rate = CryptoWebSocketHandler.class.getDeclaredMethod("rateLimitPassed", String.class);
        rate.setAccessible(true);
        assertTrue((boolean) track.invoke(handler, "X", 1.0));
        assertTrue((boolean) rate.invoke(handler, "X"));
        ReflectionTestUtils.setField(handler, "lastPrices", Map.of("X", 100.0));
        assertFalse((boolean) track.invoke(handler, "X", 100.05));
        assertTrue((boolean) track.invoke(handler, "X", 101.0));
        ReflectionTestUtils.setField(handler, "lastUpdateTimestamps", Map.of("X", System.currentTimeMillis()));
        assertFalse((boolean) rate.invoke(handler, "X"));
    }

    @Test
    void testCachePriceUpdate() throws Exception {
        Method method = CryptoWebSocketHandler.class.getDeclaredMethod("cachePriceUpdate", String.class, double.class);
        method.setAccessible(true);
        method.invoke(handler, "BTC-USD", 123.45);
        verify(valueOps).set("crypto:BTC-USD", "123.45", 1, TimeUnit.HOURS);
    }

    @Test
    void testStartBatchProcessor() {
        // prime buffer
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<String> buf = (ConcurrentLinkedQueue<String>) ReflectionTestUtils.getField(handler, "messageBuffer");
        buf.clear();
        buf.add("{A:1.0}");
        buf.add("{B:2.0}");
        ReflectionTestUtils.invokeMethod(handler, "startBatchProcessor");
        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/crypto"), cap.capture());
        String sent = cap.getValue();
        assertTrue(sent.contains("\"A\":1.0"));
        assertTrue(sent.contains("\"B\":2.0"));
    }

    @Test
    void testStartKeepAlive() {
        class StubClient extends WebSocketClient {
            boolean ping=false; StubClient(){ super(URI.create("ws://dummy")); }
            @Override public void onOpen(ServerHandshake s){
                // No-op
            }
            @Override public void onMessage(String m){
                //No-op
            }
            @Override public void onClose(int c, String r, boolean rem){
                 //No-op
            }
            @Override public void onError(Exception ex){
                 //No-op
            }
            @Override public boolean isOpen(){return true;}
            @Override public void send(String text){ if(text.contains("ping")) ping=true;}
        }
        StubClient stub = new StubClient();
        ReflectionTestUtils.setField(handler, "coinbaseWebSocket", stub);
        ReflectionTestUtils.invokeMethod(handler, "startKeepAlive");
        assertTrue(stub.ping);
    }

    @Test
    void testStartPongMonitor() {
        class StubClient extends WebSocketClient {
            boolean closed=false, open=true; StubClient(){ super(URI.create("ws://dummy")); }
            @Override public void onOpen(ServerHandshake s){
                 //No-op
            }
            @Override public void onMessage(String m){
                 //No-op
            }
            @Override public void onClose(int c, String r, boolean rem){ closed=true; }
            @Override public void onError(Exception ex){
                 //No-op
            }
            @Override public boolean isOpen(){return open;}
        }
        StubClient stub = new StubClient();
        ReflectionTestUtils.setField(handler, "coinbaseWebSocket", stub);
        ReflectionTestUtils.setField(handler, "lastPongTime", System.currentTimeMillis()-120000);
        ReflectionTestUtils.setField(handler, "connectionLostTimeoutSeconds", 1);
        ReflectionTestUtils.invokeMethod(handler, "startPongMonitor");
        assertTrue(stub.closed);
    }

    @Test
    void testReconnectDoesNotThrow() {
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(handler, "reconnectToCoinbase"));
    }
}