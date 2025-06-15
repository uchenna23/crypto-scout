package com.example.crypto_scout.Service;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@CacheConfig(cacheNames = "cryptoPrices")   // Cache configuration for storing crypto prices
public class CryptoPriceService {

    private static final Logger logger = LoggerFactory.getLogger(CryptoPriceService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String COINBASE_API_URL = "https://api.coinbase.com/v2/prices/%s/spot";
    private static final String COINGECKO_API_URL = "https://api.coingecko.com/api/v3/coins/%s/market_chart?vs_currency=%s&days=%d";
    private static final String ERROR_MESSAGE = "error";


    @Cacheable(value = "cryptoPrices", key = "#currencyPair", unless = "#result == null")
    public Map<String, Object> getCryptoPrice(String currencyPair) {
        return fetchCryptoData(currencyPair);
    }

    @Cacheable(value = "cryptoPrices", key = "#currencyPairs.toString()", unless = "#result == null")
    public Map<String, Object> getMultipleCryptoPrices(List<String> currencyPairs) {
        Map<String, Object> results = new HashMap<>();

        for (String pair : currencyPairs) {
            results.put(pair, fetchCryptoData(pair)); // Fetch and store results
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchCryptoData(String currencyPair) {
        if (currencyPair == null || currencyPair.isEmpty()) {
            return Map.of(ERROR_MESSAGE, "Invalid currency pair");
        }

        String coinbaseUrl = COINBASE_API_URL.formatted(currencyPair);

        try {
            Map<String, Object> coinbaseResponse = restTemplate.getForObject(coinbaseUrl, Map.class);

            if (coinbaseResponse == null || !coinbaseResponse.containsKey("data")) {
                return Map.of(ERROR_MESSAGE, "No data returned from Coinbase API");
            }

            Map<String, Object> data = (Map<String, Object>) coinbaseResponse.get("data");

            // Get historical data from CoinGecko
            String cryptoId = getCoinGeckoId((String) data.get("base"));
            String coingeckoUrl = COINGECKO_API_URL.formatted(cryptoId, data.get("currency"), 7);
            Map<String, Object> coingeckoResponse = restTemplate.getForObject(coingeckoUrl, Map.class);

            Map<String, Object> result = new HashMap<>();
            result.put("amount", data.get("amount"));

            if (coingeckoResponse != null && coingeckoResponse.containsKey("prices")) {
                result.put("chart", coingeckoResponse.get("prices"));
            } else {
                result.put("chart", "Historical data not available");
            }
            return result;
        } catch (Exception e) {
            logger.error("Error fetching data: {}", e.getMessage());
            return Map.of(ERROR_MESSAGE, "Failed to retrieve data");
        }
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
