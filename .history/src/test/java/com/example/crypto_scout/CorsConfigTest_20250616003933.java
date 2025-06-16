package com.example.crypto_scout;

import com.example.crypto_scout.Config.CorsConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CorsConfigTest {

    private CorsConfig corsConfig = new CorsConfig();

    @Mock
    private CorsRegistry registry;

    @Mock
    private CorsRegistration corsRegistration;

    @BeforeEach
    void setUp() {
        // Stub chained calls to return the same mock for fluent API
        when(registry.addMapping("/**")).thenReturn(corsRegistration);
        when(corsRegistration.allowedOriginPatterns("*")).thenReturn(corsRegistration);
        when(corsRegistration.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")).thenReturn(corsRegistration);
        when(corsRegistration.allowedHeaders("*")).thenReturn(corsRegistration);
        when(corsRegistration.allowCredentials(true)).thenReturn(corsRegistration);
    }

    @Test
    void corsConfigurer_shouldConfigureCorsMappings() {
        // Arrange: mock the registry to return our mock registration
        when(registry.addMapping("/**")).thenReturn(corsRegistration);

        // Act: retrieve the WebMvcConfigurer and apply CORS mappings
        WebMvcConfigurer configurer = corsConfig.corsConfigurer();
        configurer.addCorsMappings(registry);

        // Assert: verify that the registry and registration received the expected calls
        verify(registry).addMapping("/**");
        verify(corsRegistration).allowedOriginPatterns("*");
        verify(corsRegistration).allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
        verify(corsRegistration).allowedHeaders("*");
        verify(corsRegistration).allowCredentials(true);

        // Also ensure the bean method returns a non-null configurer
        assertNotNull(configurer, "The corsConfigurer bean should not be null");
    }
}
