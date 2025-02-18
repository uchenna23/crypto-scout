package com.example.crypto_scout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CryptoScoutApplication {

	public static void main(String[] args) {
		SpringApplication.run(CryptoScoutApplication.class, args);
	}

}
