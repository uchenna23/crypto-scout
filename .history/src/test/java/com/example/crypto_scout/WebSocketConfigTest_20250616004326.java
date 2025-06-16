package com.example.crypto_scout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import com.example.crypto_scout.WebSocketConfig;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    @Mock
    private StompEndpointRegistry stompEndpointRegistry;

    @Mock
    private StompWebSocketEndpointRegistration endpointRegistration;

    @BeforeEach
    void setUp() {
        // Stub fluent API for MessageBrokerRegistry
        when(messageBrokerRegistry.enableSimpleBroker("/topic")).thenReturn(messageBrokerRegistry);
        when(messageBrokerRegistry.setApplicationDestinationPrefixes("/app")).thenReturn(messageBrokerRegistry);

        // Stub fluent API for StompWebSocketEndpointRegistration
        when(stompEndpointRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOriginPatterns(
                "http://localhost:4200",
                "\"http://localhost:8080\", \"https://crypto-scout-app.azurewebsites.net\"")
        ).thenReturn(endpointRegistration);
        when(endpointRegistration.withSockJS()).thenReturn(endpointRegistration);
    }

    @Test
    void configureMessageBroker_shouldSetBrokerAndPrefix() {
        // Act
        webSocketConfig.configureMessageBroker(messageBrokerRegistry);

        // Assert
        verify(messageBrokerRegistry).enableSimpleBroker("/topic");
        verify(messageBrokerRegistry).setApplicationDestinationPrefixes("/app");
    }

    @Test
    void registerStompEndpoints_shouldAddEndpointWithSockJS() {
        // Act
        webSocketConfig.registerStompEndpoints(stompEndpointRegistry);

        // Assert
        verify(stompEndpointRegistry).addEndpoint("/ws");
        verify(endpointRegistration).setAllowedOriginPatterns(
                "http://localhost:4200",
                "\"http://localhost:8080\", \"https://crypto-scout-app.azurewebsites.net\"");
        verify(endpointRegistration).withSockJS();
    }
}
