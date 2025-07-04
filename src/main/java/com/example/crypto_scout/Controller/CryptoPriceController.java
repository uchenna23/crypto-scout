package com.example.crypto_scout.Controller;

import com.example.crypto_scout.Service.CryptoPriceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/crypto")
public class CryptoPriceController {

    private static final String ERROR = "error";
    private final CryptoPriceService cryptoPriceService;

    public CryptoPriceController(CryptoPriceService cryptoPriceService) {
        this.cryptoPriceService = cryptoPriceService;

    }

    @GetMapping("/{currencyPair}")
    public ResponseEntity<Map<String, Object>> getCryptoPrice(@PathVariable String currencyPair) {
        Map<String, Object> response = cryptoPriceService.getCryptoPrice(currencyPair);

        if (response.containsKey(ERROR)) {
            return ResponseEntity.badRequest().body(response); //  Return 400 for errors
        }

        return ResponseEntity.ok(response);
    }

    
    @GetMapping("/bulk")
    public ResponseEntity<Map<String, Object>> getBulkPrices(@RequestParam List<String> currencyPairs) {
        // Validate currency pair format
        for (String pair : currencyPairs) {
            if (!pair.matches("^[A-Za-z0-9-]+$")) {
                return ResponseEntity.badRequest().body(Map.of(ERROR, "Invalid currency pair: " + pair));
            }
        }
        Map<String, Object> response = cryptoPriceService.getMultipleCryptoPrices(currencyPairs);

        if (response.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, "No valid data returned"));
        }

        return ResponseEntity.ok(response);
    }


}
