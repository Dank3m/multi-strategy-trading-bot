package com.kinduberre.multistrategytradingbot.strategy.impl;

import com.kinduberre.multistrategytradingbot.model.MarketData;
import com.kinduberre.multistrategytradingbot.model.Signal;
import com.kinduberre.multistrategytradingbot.service.TechnicalIndicatorService;
import com.kinduberre.multistrategytradingbot.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrendFollowingStrategy implements TradingStrategy {

    private final TechnicalIndicatorService indicators;

    @Value("${trading.strategies.trend-following.sma-short}")
    private int smaShort;

    @Value("${trading.strategies.trend-following.sma-long}")
    private int smaLong;

    @Value("${trading.strategies.trend-following.breakout-period}")
    private int breakoutPeriod;

    @Value("${trading.strategies.trend-following.volume-multiplier}")
    private double volumeMultiplier;

    @Value("${trading.strategies.trend-following.atr-multiplier}")
    private double atrMultiplier;

    @Override
    public Signal analyze(List<MarketData> marketData) {
        if (marketData.size() < Math.max(smaLong, breakoutPeriod)) {
            return createHoldSignal("Insufficient data");
        }

        List<BigDecimal> closePrices = marketData.stream()
                .map(MarketData::getClose)
                .toList();

        BigDecimal sma50 = indicators.calculateSMA(closePrices, smaShort);
        BigDecimal sma200 = indicators.calculateSMA(closePrices, smaLong);
        BigDecimal currentPrice = marketData.get(marketData.size() - 1).getClose();
        BigDecimal currentVolume = marketData.get(marketData.size() - 1).getVolume();

        // Calculate 20-day high
        BigDecimal twentyDayHigh = marketData.stream()
                .skip(Math.max(0, marketData.size() - breakoutPeriod))
                .map(MarketData::getHigh)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Calculate average volume
        BigDecimal avgVolume = marketData.stream()
                .skip(Math.max(0, marketData.size() - 20))
                .map(MarketData::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(20), 8, BigDecimal.ROUND_HALF_UP);

        // Calculate ATR for stop loss
        BigDecimal atr = indicators.calculateATR(marketData, 14);

        // Entry conditions
        boolean priceAboveSMA = currentPrice.compareTo(sma50) > 0;
        boolean trendFilter = sma50.compareTo(sma200) > 0;
        boolean breakout = currentPrice.compareTo(twentyDayHigh) >= 0;
        boolean volumeConfirm = currentVolume.compareTo(
                avgVolume.multiply(BigDecimal.valueOf(volumeMultiplier))) > 0;

        if (priceAboveSMA && trendFilter && breakout && volumeConfirm) {
            Signal signal = new Signal();
            signal.setType(Signal.SignalType.BUY);
            signal.setStrategy(Signal.Strategy.TREND_FOLLOWING);
            signal.setPrice(currentPrice);
            signal.setStopLoss(currentPrice.subtract(atr.multiply(BigDecimal.valueOf(atrMultiplier))));
            signal.setTakeProfit(currentPrice.add(atr.multiply(BigDecimal.valueOf(atrMultiplier * 2))));
            signal.setReason("Trend + Breakout + Volume confirmation");
            signal.setTimestamp(LocalDateTime.now());
            signal.setConfidence(0.75);
            return signal;
        }

        // Exit condition
        if (currentPrice.compareTo(sma50) < 0) {
            Signal signal = new Signal();
            signal.setType(Signal.SignalType.SELL);
            signal.setStrategy(Signal.Strategy.TREND_FOLLOWING);
            signal.setPrice(currentPrice);
            signal.setReason("Price below SMA50 - trend exit");
            signal.setTimestamp(LocalDateTime.now());
            signal.setConfidence(0.70);
            return signal;
        }

        return createHoldSignal("No trend signal");
    }

    private Signal createHoldSignal(String reason) {
        Signal signal = new Signal();
        signal.setType(Signal.SignalType.HOLD);
        signal.setStrategy(Signal.Strategy.TREND_FOLLOWING);
        signal.setReason(reason);
        signal.setTimestamp(LocalDateTime.now());
        return signal;
    }
}
