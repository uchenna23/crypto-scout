package com.example.crypto_scout.Service;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.util.*;

@Service
@CacheConfig(cacheNames = "cryptoPrices")
public class CryptoPriceService{

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String COINBASE_API_URL = "https://api.coinbase.com/v2/prices/%s/spot";
    private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3/coins/%s/market_chart?vs_currency=%s&days=%d";
    
    @SuppressWarnings("unchecked")
    @Cacheable(value = "cryptoPrices", key = "#currencyPairs.toString()", unless = "#result == null")
    public Map<String, Object> getMultipleCryptoPrices(List<String> currencyPairs) {
        Map<String, Object> results = new HashMap<>();

        for (String pair : currencyPairs) {
            String coinbaseUrl = String.format(COINBASE_API_URL, pair);
            Map<String, Object> coinbaseResponse = restTemplate.getForObject(coinbaseUrl, Map.class);

            if (coinbaseResponse == null || !coinbaseResponse.containsKey("data")) {
                results.put(pair, Map.of("error", "Unable to fetch crypto data"));
                continue;
            }

            Map<String, Object> data = (Map<String, Object>) coinbaseResponse.get("data");

            // Get historical data from CoinGecko
            String cryptoId = getCoinGeckoId((String) data.get("base"));
            String coingeckoUrl = String.format(COINGECKO_API_URL, cryptoId, data.get("currency"), 7);
            Map<String, Object> coingeckoResponse = restTemplate.getForObject(coingeckoUrl, Map.class);

            Map<String, Object> result = new HashMap<>();
            result.put("amount", data.get("amount"));

            if (coingeckoResponse != null && coingeckoResponse.containsKey("prices")) {
                result.put("chart", coingeckoResponse.get("prices"));
            } else {
                result.put("chart", "Historical data not available");
            }

            results.put(pair, result);
        }
        return results;
    }

    private String getCoinGeckoId(String symbol) {
        Map<String, String> coinMapping = Map.of(
            "BTC", "bitcoin",
            "ETH", "ethereum",
            "ADA", "cardano",
            "XRP", "ripple",
            "DOGE", "dogecoin"
        );
        return coinMapping.getOrDefault(symbol, symbol.toLowerCase());
    }
}
