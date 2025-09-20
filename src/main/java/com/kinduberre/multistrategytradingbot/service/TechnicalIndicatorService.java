package com.kinduberre.multistrategytradingbot.service;

import com.kinduberre.multistrategytradingbot.model.MarketData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class TechnicalIndicatorService {

    public BigDecimal calculateSMA(List<BigDecimal> prices, int period) {
        if (prices.size() < period) return null;

        BigDecimal sum = BigDecimal.ZERO;
        for (int i = prices.size() - period; i < prices.size(); i++) {
            sum = sum.add(prices.get(i));
        }
        return sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateEMA(List<BigDecimal> prices, int period) {
        if (prices.size() < period) return null;

        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal ema = calculateSMA(prices.subList(0, period), period);

        for (int i = period; i < prices.size(); i++) {
            ema = prices.get(i).multiply(multiplier)
                    .add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
        }
        return ema;
    }

    public BigDecimal calculateATR(List<MarketData> candles, int period) {
        if (candles.size() < period + 1) return null;

        List<BigDecimal> trueRanges = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            BigDecimal high = candles.get(i).getHigh();
            BigDecimal low = candles.get(i).getLow();
            BigDecimal prevClose = candles.get(i-1).getClose();

            BigDecimal tr1 = high.subtract(low);
            BigDecimal tr2 = high.subtract(prevClose).abs();
            BigDecimal tr3 = low.subtract(prevClose).abs();

            trueRanges.add(tr1.max(tr2).max(tr3));
        }

        return calculateSMA(trueRanges, period);
    }

    public BollingerBands calculateBollingerBands(List<BigDecimal> prices, int period, double stdDev) {
        if (prices.size() < period) return null;

        BigDecimal sma = calculateSMA(prices, period);
        BigDecimal variance = BigDecimal.ZERO;

        for (int i = prices.size() - period; i < prices.size(); i++) {
            BigDecimal diff = prices.get(i).subtract(sma);
            variance = variance.add(diff.multiply(diff));
        }

        BigDecimal std = BigDecimal.valueOf(Math.sqrt(
                variance.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP).doubleValue()
        ));

        return new BollingerBands(
                sma.add(std.multiply(BigDecimal.valueOf(stdDev))),  // upper
                sma,  // middle
                sma.subtract(std.multiply(BigDecimal.valueOf(stdDev)))  // lower
        );
    }

    @Data
    @AllArgsConstructor
    public static class BollingerBands {
        private BigDecimal upper;
        private BigDecimal middle;
        private BigDecimal lower;
    }

    public BigDecimal calculateRSI(List<BigDecimal> prices, int period) {
        if (prices.size() < period + 1) return null;

        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        for (int i = 1; i <= period; i++) {
            BigDecimal change = prices.get(i).subtract(prices.get(i-1));
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                avgGain = avgGain.add(change);
            } else {
                avgLoss = avgLoss.add(change.abs());
            }
        }

        avgGain = avgGain.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);

        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.valueOf(100);

        BigDecimal rs = avgGain.divide(avgLoss, 8, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 8, RoundingMode.HALF_UP)
        );
    }
}