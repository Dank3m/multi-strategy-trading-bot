package com.kinduberre.multistrategytradingbot.service;

import com.kinduberre.multistrategytradingbot.model.MarketData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BinanceService {

    @Value("${binance.api.key}")
    private String apiKey;

    @Value("${binance.api.secret}")
    private String secretKey;

    @Value("${binance.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public BinanceService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<MarketData> getKlines(String symbol, String interval, int limit) {
        String url = baseUrl + "/api/v3/klines?symbol=" + symbol +
                "&interval=" + interval + "&limit=" + limit;

        try {
            Object[] response = restTemplate.getForObject(url, Object[].class);
            List<MarketData> marketDataList = new ArrayList<>();

            for (Object kline : response) {
                List<Object> data = (List<Object>) kline;
                MarketData md = new MarketData();
                md.setSymbol(symbol);
                md.setOpen(new BigDecimal(data.get(1).toString()));
                md.setHigh(new BigDecimal(data.get(2).toString()));
                md.setLow(new BigDecimal(data.get(3).toString()));
                md.setClose(new BigDecimal(data.get(4).toString()));
                md.setVolume(new BigDecimal(data.get(5).toString()));
                md.setVolumeUsdt(new BigDecimal(data.get(7).toString()));
                md.setTradeCount(Long.parseLong(data.get(8).toString()));
                md.setTimestamp(LocalDateTime.now()); // Convert from epoch
                marketDataList.add(md);
            }

            return marketDataList;
        } catch (Exception e) {
            log.error("Error fetching klines: ", e);
            return new ArrayList<>();
        }
    }

    public BigDecimal getAccountBalance(String asset) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String queryString = "timestamp=" + timestamp;
        String signature = generateSignature(queryString);

        String url = baseUrl + "/api/v3/account?" + queryString + "&signature=" + signature;

        Map<String, String> headers = new HashMap<>();
        headers.put("X-MBX-APIKEY", apiKey);

        try {
            Map response = restTemplate.getForObject(url, Map.class);
            List<Map> balances = (List<Map>) response.get("balances");

            for (Map balance : balances) {
                if (asset.equals(balance.get("asset"))) {
                    return new BigDecimal(balance.get("free").toString());
                }
            }
        } catch (Exception e) {
            log.error("Error getting balance: ", e);
        }

        return BigDecimal.ZERO;
    }

    public String placeOrder(String symbol, String side, BigDecimal quantity, BigDecimal price) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String queryString = "symbol=" + symbol + "&side=" + side +
                "&type=LIMIT&timeInForce=GTC" +
                "&quantity=" + quantity + "&price=" + price +
                "&timestamp=" + timestamp;
        String signature = generateSignature(queryString);

        String url = baseUrl + "/api/v3/order?" + queryString + "&signature=" + signature;

        Map<String, String> headers = new HashMap<>();
        headers.put("X-MBX-APIKEY", apiKey);

        try {
            Map response = restTemplate.postForObject(url, null, Map.class);
            return response.get("orderId").toString();
        } catch (Exception e) {
            log.error("Error placing order: ", e);
            return null;
        }
    }

    private String generateSignature(String data) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes());
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate signature", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}