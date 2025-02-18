package com.example.crypto_scout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import java.util.Collections;

@SpringBootApplication
@EnableCaching
public class CryptoScoutApplication {

    public static void main(String[] args) {
        // Retrieve the PORT environment variable
        String port = System.getenv("PORT");
        if (port == null) {
            port = "8080"; // default if PORT is not set
        }
        System.out.println("Starting application on port: " + port);

        // Set the server.port property to the value from the environment variable
        SpringApplication app = new SpringApplication(CryptoScoutApplication.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", port));
        app.run(args);
    }
}
