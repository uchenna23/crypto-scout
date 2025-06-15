package com.example.crypto_scout;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import com.example.crypto_scout.Service.OpenAIService;

@TestConfiguration
public class OpenAITestConfig {

    @Bean
    OpenAIService mockOpenAIService() {
        return Mockito.mock(OpenAIService.class);
    }
}