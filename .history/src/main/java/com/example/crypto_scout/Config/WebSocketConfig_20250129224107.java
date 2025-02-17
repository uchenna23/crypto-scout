package com.example.crypto_scout.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer{

    private final CryptoWebSocketHandler cryptoWebSocketHandler;

    public WebSocketConfig(CryptoWebSocketHandler cryptoWebSocketHandler){
        this.cryptoWebSocketHandler = cryptoWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){
        registry.addHandler(cryptoWebSocketHandler,"/ws/crypto").setAllowedOrigins("*");
    }
    
}
