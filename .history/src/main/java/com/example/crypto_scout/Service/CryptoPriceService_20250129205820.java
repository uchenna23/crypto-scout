package com.example.crypto_scout.Service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class CryptoPriceService{

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String COINBASE_API_URL = "https://api.coinbase.com/v2/prices/%s/spot";
    private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3/coins/%s/market_chart?vs_currency=%s&days=%d";
    
    @SuppressWarnings("unchecked")
    @Cacheable(value = "cryptoPrices", key = "#currencyPair", unless = "#result == null")
    public Map<String, Object> getCryptoPrice(String currencyPair) {
        String coinbaseUrl = String.format(COINBASE_API_URL, currencyPair);
        Map<String, Object> coinbaseResponse = restTemplate.getForObject(coinbaseUrl, Map.class);

        if (coinbaseResponse == null || !coinbaseResponse.containsKey("data")) {
            return Map.of("error", "Unable to fetch crypto data");
        }

        Map<String, Object> data = (Map<String, Object>) coinbaseResponse.get("data");

        // Get historical data from CoinGecko
        String cryptoId = getCoinGeckoId((String) data.get("base")); // Convert "BTC" -> "bitcoin"
        String coingeckoUrl = String.format(COINGECKO_API_URL, cryptoId, data.get("currency"), 7);
        Map<String, Object> coingeckoResponse = restTemplate.getForObject(coingeckoUrl, Map.class);

        Map<String, Object> result = new HashMap<>();
        result.put("amount", data.get("amount")); // Price

        if (coingeckoResponse != null && coingeckoResponse.containsKey("prices")) {
            result.put("chart", coingeckoResponse.get("prices")); // Price history (timestamp, price)
        } else {
            result.put("chart", "Historical data not available");
        }

        return result;
    }

    // Helper method to map ticker symbols to CoinGecko's IDs
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
