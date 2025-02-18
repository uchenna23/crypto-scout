package com.example.crypto_scout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import java.util.Collections;

@SpringBootApplication
@EnableCaching
public class CryptoScoutApplication {

    public static void main(String[] args) {
        String portEnv = System.getenv("PORT");
        System.out.println("Environment variable PORT: " + portEnv);

        // Force the server port using a system property
        SpringApplication app = new SpringApplication(CryptoScoutApplication.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", "80"));
        app.run(args);
    }
}
