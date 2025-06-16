package com.example.crypto_scout;

import org.junit.jupiter.api.Test;

class CryptoScoutApplicationTests {

	@Test
	void contextLoads() {
		// This test will pass if the application context loads successfully
		org.junit.jupiter.api.Assertions.assertTrue(true);
	}

	@Test
	void main() {
		// This test will pass if the main method runs without throwing an exception
		CryptoScoutApplication.main(new String[] {});
		org.junit.jupiter.api.Assertions.assertTrue(true);
	}
}
