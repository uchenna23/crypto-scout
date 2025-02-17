package com.example.crypto_scout.Service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
public class CryptoPriceService{

    private final RestTemplate restTemplate = new RestTemplate();
    private final String COIN_BASE_URL = "https://api.coinbase.com/v2/prices/%s/spot";

    @SuppressWarnings("unchecked")
    @Cacheable(value = "cryptoPrices", key = "#currencyPair", unless = "#result == null")
    public Map<String, Object> getCryptoPrices(String currencyPair){
        String url = String.format(COIN_BASE_URL, currencyPair);
        return restTemplate.getForObject(url, Map.class);
    }
}