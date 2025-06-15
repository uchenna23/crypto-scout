package com.example.crypto_scout;

import com.example.crypto_scout.Service.CryptoWebSocketHandler;
import com.example.crypto_scout.Service.CryptoWebSocketHandler.CryptoWebSocketInitException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.Framedata;
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

@SuppressWarnings("unchecked")
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
        @Override public void execute(Runnable command) { command.run(); }
        @Override public Future<?> submit(Runnable command) { command.run(); return CompletableFuture.completedFuture(null); }
        @Override public <T> Future<T> submit(Runnable command, T result) { command.run(); return CompletableFuture.completedFuture(result); }
        @Override public <T> Future<T> submit(Callable<T> task) {
            try { return CompletableFuture.completedFuture(task.call()); }
            catch (Exception e) {
                CompletableFuture<T> future = new CompletableFuture<>(); future.completeExceptionally(e); return future;
            }
        }
        @Override public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
        @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) { throw new UnsupportedOperationException(); }
        @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) { throw new UnsupportedOperationException(); }
        @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) { throw new UnsupportedOperationException(); }
        @Override public boolean isShutdown() { return false; }
        @Override public boolean isTerminated() { return false; }
        @Override public void shutdown() { 
            //No-op
        }
        @Override public List<Runnable> shutdownNow() { return Collections.emptyList(); }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        handler = new CryptoWebSocketHandler(messagingTemplate, redisTemplate);
        ReflectionTestUtils.setField(handler, "scheduler", new ImmediateScheduledExecutorService());
        ReflectionTestUtils.setField(handler, "coinbaseWebSocketUri", "wss://echo.websocket.events");
        ReflectionTestUtils.setField(handler, "apiKey", "dummy");
    }

    @Test
    void testInitThrowsException() {
        ReflectionTestUtils.setField(handler, "coinbaseWebSocketUri", null);
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
        Method m = CryptoWebSocketHandler.class.getDeclaredMethod("sendSubscriptionRequest", WebSocketClient.class);
        m.setAccessible(true);
        m.invoke(handler, client);
        assertEquals(1, sent.size());
        assertTrue(sent.get(0).contains("\"type\":\"subscribe\""));
    }

    @Test
    void testProcessIncomingMessagePaths() throws Exception {
        Method m = CryptoWebSocketHandler.class.getDeclaredMethod("processIncomingMessage", String.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(handler, "not-json"));
        assertDoesNotThrow(() -> m.invoke(handler, "{}"));
        assertDoesNotThrow(() -> m.invoke(handler, "{\"events\":123}"));
    }

    @Test
    void testProcessIncomingMessageHappyPath() throws Exception {
        String msg = "{\"events\":[{\"tickers\":[{\"product_id\":\"BTC-USD\",\"price\":\"100.5\"}]}]}";
        ((Map<String, Double>) ReflectionTestUtils.getField(handler, "lastPrices")).clear();
        ((Map<String, Long>) ReflectionTestUtils.getField(handler, "lastUpdateTimestamps")).clear();
        Method m = CryptoWebSocketHandler.class.getDeclaredMethod("processIncomingMessage", String.class);
        m.setAccessible(true);
        m.invoke(handler, msg);
        
        ConcurrentLinkedQueue<String> buf = (ConcurrentLinkedQueue<String>) ReflectionTestUtils.getField(handler, "messageBuffer");
        assertEquals("{ \"BTC-USD\": 100.5 }", buf.poll());
    }

    @Test
    void testTrackPriceChangeRateLimit() throws Exception {
        Method tr = CryptoWebSocketHandler.class.getDeclaredMethod("trackPriceChange", String.class, double.class);
        Method rl = CryptoWebSocketHandler.class.getDeclaredMethod("rateLimitPassed", String.class);
        tr.setAccessible(true); rl.setAccessible(true);
        assertTrue((boolean) tr.invoke(handler, "X", 1.0));
        assertTrue((boolean) rl.invoke(handler, "X"));
        ((Map<String, Double>) ReflectionTestUtils.getField(handler, "lastPrices")).put("X", 100.0);
        assertFalse((boolean) tr.invoke(handler, "X", 100.05));
        assertTrue((boolean) tr.invoke(handler, "X", 101.0));
        Map<String, Long> ts = new ConcurrentHashMap<>(); ts.put("X", System.currentTimeMillis());
        ReflectionTestUtils.setField(handler, "lastUpdateTimestamps", ts);
        assertFalse((boolean) rl.invoke(handler, "X"));
    }

    @Test
    void testCachePriceUpdate() throws Exception {
        Method m = CryptoWebSocketHandler.class.getDeclaredMethod("cachePriceUpdate", String.class, double.class);
        m.setAccessible(true);
        m.invoke(handler, "BTC-USD", 123.45);
        verify(valueOps).set("crypto:BTC-USD", "123.45", 1, TimeUnit.HOURS);
    }

    @Test
    void testStartBatchProcessor() {
        ConcurrentLinkedQueue<String> buf = (ConcurrentLinkedQueue<String>) ReflectionTestUtils.getField(handler, "messageBuffer");
        buf.clear(); buf.add("{\"A\":1.0}"); buf.add("{\"B\":2.0}");
        ReflectionTestUtils.invokeMethod(handler, "startBatchProcessor");
        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/crypto"), cap.capture());
        String sent = cap.getValue();
        assertTrue(sent.contains("\"A\":1.0"));
    }

    @Test
    void testStartKeepAlive() {
        class Stub extends WebSocketClient { boolean ping=false; Stub(){ super(URI.create("ws://dummy")); }
            @Override public void onOpen(ServerHandshake s){
                //No-op
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
            @Override public void send(String t){ if(t.contains("ping")) ping=true; }
        }
        Stub stub = new Stub();
        ReflectionTestUtils.setField(handler, "coinbaseWebSocket", stub);
        ReflectionTestUtils.invokeMethod(handler, "startKeepAlive");
        assertTrue(stub.ping);
    }

    @Test
    void testStartPongMonitor() {
        class Stub extends WebSocketClient { boolean closed=false; Stub(){ super(URI.create("ws://dummy"));}
            @Override public void onOpen(ServerHandshake s){
                //No-op
            }
            @Override public void onMessage(String m){
                //No-op
            }
            @Override public void onError(Exception ex){
                //No-op
            }
            @Override public boolean isOpen(){return true;}
            @Override public void close(){ closed=true; }
            @Override public void onClose(int c, String r, boolean rem){ closed=true; }
        }
        Stub stub = new Stub();
        ReflectionTestUtils.setField(handler, "coinbaseWebSocket", stub);
        ReflectionTestUtils.setField(handler, "lastPongTime", System.currentTimeMillis()-120_000);
        ReflectionTestUtils.setField(handler, "connectionLostTimeoutSeconds", 1);
        ReflectionTestUtils.invokeMethod(handler, "startPongMonitor");
        assertTrue(stub.closed);
    }

    @Test
    void testReconnectDoesNotThrow() {
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(handler, "reconnectToCoinbase"));
    }

    @Test
    void testInitSuccess() throws Exception {
        // Should initialize without throwing and set up WebSocket client
        handler.init();
        Object client = ReflectionTestUtils.getField(handler, "coinbaseWebSocket");
        assertNotNull(client, "init() should instantiate coinbaseWebSocket");
    }// ===== additional WebSocketClient callback tests =====

    @Test
    void testWebSocketClientOnOpen() {
        ReflectionTestUtils.invokeMethod(handler, "connectToCoinbase");
        WebSocketClient client = (WebSocketClient) ReflectionTestUtils.getField(handler, "coinbaseWebSocket");
        long before = (long) ReflectionTestUtils.getField(handler, "lastPongTime");
        client.onOpen(mock(ServerHandshake.class));
        long after = (long) ReflectionTestUtils.getField(handler, "lastPongTime");
        assertTrue(after >= before);
    }

    @Test
    void testWebSocketClientOnMessage() {
        ReflectionTestUtils.setField(handler, "messageProcessingExecutor", new ImmediateScheduledExecutorService());
        ReflectionTestUtils.invokeMethod(handler, "connectToCoinbase");
        WebSocketClient client = (WebSocketClient) ReflectionTestUtils.getField(handler, "coinbaseWebSocket");
        // invalid JSON path
        client.onMessage("abc");
        // valid JSON path
        client.onMessage("{\"events\":[{\"tickers\":[{\"product_id\":\"Z-USD\",\"price\":\"5\"}]}]}");
        ConcurrentLinkedQueue<String> buf = (ConcurrentLinkedQueue<String>) ReflectionTestUtils.getField(handler, "messageBuffer");
        assertTrue(buf.contains("{ \"Z-USD\": 5.0 }"));
    }

    @Test
    void testWebSocketClientOnWebsocketPong(){
        ReflectionTestUtils.invokeMethod(handler, "connectToCoinbase");
        WebSocketClient client = (WebSocketClient) ReflectionTestUtils.getField(handler, "coinbaseWebSocket");
        long before = (long) ReflectionTestUtils.getField(handler, "lastPongTime");
        client.onWebsocketPong(client, mock(Framedata.class));
        long after = (long) ReflectionTestUtils.getField(handler, "lastPongTime");
        assertTrue(after >= before);
    }

    @Test
    void testWebSocketClientOnErrorDoesNotThrow() {
        ReflectionTestUtils.invokeMethod(handler, "connectToCoinbase");
        WebSocketClient client = (WebSocketClient) ReflectionTestUtils.getField(handler, "coinbaseWebSocket");
        assertDoesNotThrow(() -> client.onError(new Exception("err")));
    }

    @Test
    void testWebSocketClientOnCloseSchedulesReconnect() {
        ScheduledExecutorService sched = mock(ScheduledExecutorService.class);
        ReflectionTestUtils.setField(handler, "scheduler", sched);
        ReflectionTestUtils.invokeMethod(handler, "connectToCoinbase");
        WebSocketClient client = (WebSocketClient) ReflectionTestUtils.getField(handler, "coinbaseWebSocket");
        client.onClose(100, "bye", true);
        verify(sched).schedule(any(Runnable.class), eq(60L), eq(TimeUnit.SECONDS));
    }
}
