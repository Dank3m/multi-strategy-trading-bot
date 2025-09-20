package com.kinduberre.multistrategytradingbot.strategy.impl;

import com.kinduberre.multistrategytradingbot.model.MarketData;
import com.kinduberre.multistrategytradingbot.model.Signal;
import com.kinduberre.multistrategytradingbot.service.TechnicalIndicatorService;
import com.kinduberre.multistrategytradingbot.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RangeMeanReversionStrategy implements TradingStrategy {
    private final TechnicalIndicatorService indicators;

    @Value("${trading.strategies.mean-reversion.range-period}")
    private int rangePeriod;

    @Value("${trading.strategies.mean-reversion.bollinger-period}")
    private int bollingerPeriod;

    @Value("${trading.strategies.mean-reversion.bollinger-std}")
    private double bollingerStd;

    @Value("${trading.strategies.mean-reversion.volume-multiplier}")
    private double volumeMultiplier;

    @Override
    public Signal analyze(List<MarketData> marketData) {
        if (marketData.size() < Math.max(rangePeriod, bollingerPeriod)) {
            return createHoldSignal("Insufficient data");
        }

        // Detect if we're in a range
        BigDecimal rangeHigh = marketData.stream()
                .skip(Math.max(0, marketData.size() - rangePeriod))
                .map(MarketData::getHigh)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal rangeLow = marketData.stream()
                .skip(Math.max(0, marketData.size() - rangePeriod))
                .map(MarketData::getLow)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal meanPrice = marketData.stream()
                .skip(Math.max(0, marketData.size() - rangePeriod))
                .map(MarketData::getClose)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(rangePeriod), 8, RoundingMode.HALF_UP);

        BigDecimal rangeSize = rangeHigh.subtract(rangeLow);
        BigDecimal rangePercent = rangeSize.divide(meanPrice, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        // Only trade if range is between 5% and 15% (configurable)
        boolean inRange = rangePercent.compareTo(BigDecimal.valueOf(5)) > 0 &&
                rangePercent.compareTo(BigDecimal.valueOf(15)) < 0;

        if (inRange) {
            List<BigDecimal> closePrices = marketData.stream()
                    .map(MarketData::getClose)
                    .toList();

            TechnicalIndicatorService.BollingerBands bb = indicators.calculateBollingerBands(
                    closePrices, bollingerPeriod, bollingerStd
            );

            MarketData current = marketData.get(marketData.size() - 1);
            BigDecimal currentPrice = current.getClose();

            // Check volume (should be low for mean reversion)
            BigDecimal avgVolume = marketData.stream()
                    .skip(Math.max(0, marketData.size() - 20))
                    .map(MarketData::getVolume)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(20), 8, RoundingMode.HALF_UP);

            boolean lowVolume = current.getVolume().compareTo(avgVolume) < 0;

            // Sell signal at upper band
            if (currentPrice.compareTo(bb.getUpper()) >= 0 && lowVolume) {
                Signal signal = new Signal();
                signal.setType(Signal.SignalType.SELL);
                signal.setStrategy(Signal.Strategy.MEAN_REVERSION);
                signal.setPrice(currentPrice);
                signal.setStopLoss(rangeHigh.multiply(BigDecimal.valueOf(1.02)));
                signal.setTakeProfit(bb.getMiddle());
                signal.setReason("Price at upper Bollinger Band in range");
                signal.setTimestamp(LocalDateTime.now());
                signal.setConfidence(0.65);
                return signal;
            }

            // Buy signal at lower band
            if (currentPrice.compareTo(bb.getLower()) <= 0 && lowVolume) {
                Signal signal = new Signal();
                signal.setType(Signal.SignalType.BUY);
                signal.setStrategy(Signal.Strategy.MEAN_REVERSION);
                signal.setPrice(currentPrice);
                signal.setStopLoss(rangeLow.multiply(BigDecimal.valueOf(0.98)));
                signal.setTakeProfit(bb.getMiddle());
                signal.setReason("Price at lower Bollinger Band in range");
                signal.setTimestamp(LocalDateTime.now());
                signal.setConfidence(0.65);
                return signal;
            }
        }

        return createHoldSignal("No mean reversion opportunity");
    }

    private Signal createHoldSignal(String reason) {
        Signal signal = new Signal();
        signal.setType(Signal.SignalType.HOLD);
        signal.setStrategy(Signal.Strategy.MEAN_REVERSION);
        signal.setReason(reason);
        signal.setTimestamp(LocalDateTime.now());
        return signal;
    }
}
