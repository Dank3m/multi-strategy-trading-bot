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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VolatilityBreakoutStrategy implements TradingStrategy {

    private final TechnicalIndicatorService indicators;

    @Value("${trading.strategies.volatility-breakout.atr-period}")
    private int atrPeriod;

    @Value("${trading.strategies.volatility-breakout.compression-percentile}")
    private double compressionPercentile;

    @Value("${trading.strategies.volatility-breakout.volume-multiplier}")
    private double volumeMultiplier;

    @Value("${trading.strategies.volatility-breakout.reward-risk-ratio}")
    private double rewardRiskRatio;

    @Override
    public Signal analyze(List<MarketData> marketData) {
        if (marketData.size() < 50) {
            return createHoldSignal("Insufficient data");
        }

        // Calculate current and historical ATR
        BigDecimal currentATR = indicators.calculateATR(
                marketData.subList(marketData.size() - atrPeriod - 1, marketData.size()),
                atrPeriod
        );

        List<BigDecimal> historicalATRs = new ArrayList<>();
        for (int i = atrPeriod + 1; i < marketData.size(); i++) {
            BigDecimal atr = indicators.calculateATR(
                    marketData.subList(i - atrPeriod - 1, i),
                    atrPeriod
            );
            if (atr != null) historicalATRs.add(atr);
        }

        // Sort ATRs to find percentile
        historicalATRs.sort(BigDecimal::compareTo);
        int percentileIndex = (int) (historicalATRs.size() * compressionPercentile);
        BigDecimal compressionThreshold = historicalATRs.get(percentileIndex);

        boolean isCompressed = currentATR.compareTo(compressionThreshold) < 0;

        if (isCompressed) {
            // Look for breakout
            MarketData current = marketData.get(marketData.size() - 1);

            // Find consolidation high/low (last 14 days)
            BigDecimal consolidationHigh = marketData.stream()
                    .skip(Math.max(0, marketData.size() - 14))
                    .map(MarketData::getHigh)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal consolidationLow = marketData.stream()
                    .skip(Math.max(0, marketData.size() - 14))
                    .map(MarketData::getLow)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            // Check volume
            BigDecimal avgVolume = marketData.stream()
                    .skip(Math.max(0, marketData.size() - 20))
                    .map(MarketData::getVolume)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(20), 8, RoundingMode.HALF_UP);

            boolean volumeBreakout = current.getVolume().compareTo(
                    avgVolume.multiply(BigDecimal.valueOf(volumeMultiplier))) > 0;

            // Breakout signals
            if (current.getClose().compareTo(consolidationHigh) > 0 && volumeBreakout) {
                Signal signal = new Signal();
                signal.setType(Signal.SignalType.BUY);
                signal.setStrategy(Signal.Strategy.VOLATILITY_BREAKOUT);
                signal.setPrice(current.getClose());
                signal.setStopLoss(consolidationLow);

                BigDecimal risk = current.getClose().subtract(consolidationLow);
                signal.setTakeProfit(current.getClose().add(
                        risk.multiply(BigDecimal.valueOf(rewardRiskRatio))
                ));

                signal.setReason("Volatility compression breakout with volume");
                signal.setTimestamp(LocalDateTime.now());
                signal.setConfidence(0.80);
                return signal;
            }
        }

        return createHoldSignal("No volatility breakout");
    }

    private Signal createHoldSignal(String reason) {
        Signal signal = new Signal();
        signal.setType(Signal.SignalType.HOLD);
        signal.setStrategy(Signal.Strategy.VOLATILITY_BREAKOUT);
        signal.setReason(reason);
        signal.setTimestamp(LocalDateTime.now());
        return signal;
    }
}