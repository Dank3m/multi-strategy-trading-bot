package com.kinduberre.multistrategytradingbot.service;

import com.kinduberre.multistrategytradingbot.model.MarketData;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class MarketDataService {
    // This is a mock implementation - replace with actual market data API
    public List<MarketData> getLatestMarketData(int periods) {
        List<MarketData> data = new ArrayList<>();
        Random random = new Random();

        BigDecimal basePrice = new BigDecimal("50000");

        for (int i = 0; i < periods; i++) {
            BigDecimal change = new BigDecimal(random.nextGaussian() * 1000);
            BigDecimal open = basePrice.add(change);
            BigDecimal close = open.add(new BigDecimal(random.nextGaussian() * 500));
            BigDecimal high = open.max(close).add(new BigDecimal(Math.abs(random.nextGaussian() * 200)));
            BigDecimal low = open.min(close).subtract(new BigDecimal(Math.abs(random.nextGaussian() * 200)));

            data.add(MarketData.builder()
                    .timestamp(LocalDateTime.now().minusDays(i))
                    .open(open)
                    .high(high)
                    .low(low)
                    .close(close)
                    .volume(new BigDecimal(random.nextInt(10000) + 1000))
                    .volumeUsdt(new BigDecimal(random.nextInt(50000000) + 10000000))
                    .tradeCount((long)(random.nextInt(100000) + 10000))
                    .build());

            basePrice = close;
        }

        return data;
    }
}
