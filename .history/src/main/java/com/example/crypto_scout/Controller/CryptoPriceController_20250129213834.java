package com.example.crypto_scout.Controller;

import com.example.crypto_scout.Service.CryptoPriceService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
@RequestMapping("/api/crypto")
public class CryptoPriceController {
    
    private final CryptoPriceService cryptoPriceService;

    public CryptoPriceController(CryptoPriceService cryptoPriceService){
        this.cryptoPriceService = cryptoPriceService;
    }

    @GetMapping("/{currencyPair}")
    public Map<String, Object> getCryptoPrice(@PathVariable String currencyPair) {
        return cryptoPriceService.getCryptoPrice(currencyPair);
    }
    
    @GetMapping("/bulk")
    public Map<String, Object> getBulkPrices(@RequestParam List<String> currencyPairs){
        Map<String, Object> result = new HashMap<>();
        for(String pair : currencyPairs){
            result.put(pair, cryptoPriceService.getCryptoPrice(pair));
        }
        return result;
    }
}
