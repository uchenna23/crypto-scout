package com.example.crypto_scout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.example.crypto_scout.Controller.WebSocketTestController;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class WebSocketTestControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketTestController controller;

    @Test
    void sendTestMessage_shouldSendMessageAndReturnConfirmation() {
        // Act
        String response = controller.sendTestMessage();

        // Assert: verify messagingTemplate was called correctly
        ArgumentCaptor<Map<String, Double>> captor = ArgumentCaptor.forClass((Class) Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/crypto"), captor.capture());

        Map<String, Double> sentMessage = captor.getValue();
        assertEquals(1, sentMessage.size(), "The message map should contain exactly one entry");
        assertTrue(sentMessage.containsKey("BTC-USD"), "The message map should contain the key 'BTC-USD'");
        assertEquals(45000.12, sentMessage.get("BTC-USD"), 0.0001, "The message value for 'BTC-USD' should match");

        // Assert: verify return value
        assertEquals(
            "📩 Sent to WebSocket Clients: {\"BTC-USD\":45000.12}",
            response,
            "The controller should return the confirmation string with the exact payload"
        );
    }
}
