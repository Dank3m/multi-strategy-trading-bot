package com.kinduberre.multistrategytradingbot.strategy.impl;

import com.kinduberre.multistrategytradingbot.model.MarketData;
import com.kinduberre.multistrategytradingbot.model.Signal;
import com.kinduberre.multistrategytradingbot.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VolumeSpikeReversalStrategy implements TradingStrategy {

    @Value("${trading.strategies.volume-spike.volume-multiplier}")
    private double volumeMultiplier;

    @Value("${trading.strategies.volume-spike.wick-ratio}")
    private double wickRatio;

    @Override
    public Signal analyze(List<MarketData> marketData) {
        if (marketData.size() < 20) {
            return createHoldSignal("Insufficient data");
        }

        MarketData current = marketData.get(marketData.size() - 1);

        // Calculate average volume
        BigDecimal avgVolume = marketData.stream()
                .skip(Math.max(0, marketData.size() - 20))
                .map(MarketData::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(20), 8, RoundingMode.HALF_UP);

        // Check for volume spike
        boolean volumeSpike = current.getVolume().compareTo(
                avgVolume.multiply(BigDecimal.valueOf(volumeMultiplier))) > 0;

        if (volumeSpike) {
            BigDecimal range = current.getHigh().subtract(current.getLow());
            BigDecimal body = current.getClose().subtract(current.getOpen()).abs();

            // Check for upper wick (potential short)
            BigDecimal upperWick = current.getHigh().subtract(
                    current.getClose().max(current.getOpen())
            );

            if (upperWick.compareTo(range.multiply(BigDecimal.valueOf(wickRatio))) > 0) {
                Signal signal = new Signal();
                signal.setType(Signal.SignalType.SELL);
                signal.setStrategy(Signal.Strategy.VOLUME_SPIKE_REVERSAL);
                signal.setPrice(current.getClose());
                signal.setStopLoss(current.getHigh().multiply(BigDecimal.valueOf(1.01)));
                signal.setTakeProfit(current.getClose().subtract(range));
                signal.setReason("Volume spike with upper wick rejection");
                signal.setTimestamp(LocalDateTime.now());
                signal.setConfidence(0.60);
                return signal;
            }

            // Check for lower wick (potential long)
            BigDecimal lowerWick = current.getClose().min(current.getOpen())
                    .subtract(current.getLow());

            if (lowerWick.compareTo(range.multiply(BigDecimal.valueOf(wickRatio))) > 0) {
                Signal signal = new Signal();
                signal.setType(Signal.SignalType.BUY);
                signal.setStrategy(Signal.Strategy.VOLUME_SPIKE_REVERSAL);
                signal.setPrice(current.getClose());
                signal.setStopLoss(current.getLow().multiply(BigDecimal.valueOf(0.99)));
                signal.setTakeProfit(current.getClose().add(range));
                signal.setReason("Volume spike with lower wick rejection");
                signal.setTimestamp(LocalDateTime.now());
                signal.setConfidence(0.60);
                return signal;
            }
        }

        return createHoldSignal("No volume spike reversal");
    }

    private Signal createHoldSignal(String reason) {
        Signal signal = new Signal();
        signal.setType(Signal.SignalType.HOLD);
        signal.setStrategy(Signal.Strategy.VOLUME_SPIKE_REVERSAL);
        signal.setReason(reason);
        signal.setTimestamp(LocalDateTime.now());
        return signal;
    }
}