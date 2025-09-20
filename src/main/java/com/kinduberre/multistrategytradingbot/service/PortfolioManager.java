package com.kinduberre.multistrategytradingbot.service;

import com.kinduberre.multistrategytradingbot.config.TradingConfig;
import com.kinduberre.multistrategytradingbot.model.Position;
import com.kinduberre.multistrategytradingbot.model.Signal;
import com.kinduberre.multistrategytradingbot.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioManager {
    private final TradingConfig config;
    private final PositionRepository positionRepository;

    public BigDecimal calculatePositionSize(Signal signal, BigDecimal currentPrice) {
        BigDecimal allocation = getStrategyAllocation(signal.getStrategy());
        BigDecimal availableCapital = config.getAccountBalance().multiply(allocation);
        BigDecimal riskAmount = availableCapital.multiply(config.getRiskPerTrade());

        BigDecimal stopDistance = currentPrice.subtract(signal.getStopLoss()).abs();
        if (stopDistance.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal positionSize = riskAmount.divide(stopDistance, 8, RoundingMode.DOWN);

        // Check against maximum position size
        BigDecimal maxPosition = availableCapital.divide(currentPrice, 8, RoundingMode.DOWN);

        return positionSize.min(maxPosition);
    }

    private BigDecimal getStrategyAllocation(Signal.Strategy strategy) {
        switch (strategy) {
            case TREND_FOLLOWING:
                return config.getAllocation().getTrendFollowing();
            case VOLATILITY_BREAKOUT:
                return config.getAllocation().getVolatilityBreakout();
            case MEAN_REVERSION:
                return config.getAllocation().getRangeReversion();
            case VOLUME_SPIKE_REVERSAL:
                return config.getAllocation().getVolumeSpikeReversal();
            default:
                return new BigDecimal("0.1");
        }
    }

    public boolean checkRiskLimits() {
        List<Position> openPositions = positionRepository.findByIsOpen(true);

        BigDecimal totalExposure = openPositions.stream()
                .map(p -> p.getQuantity().multiply(p.getEntryPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal exposureRatio = totalExposure.divide(
                config.getAccountBalance(), 8, RoundingMode.HALF_UP
        );

        // Check if we're within risk limits
        return exposureRatio.compareTo(BigDecimal.ONE.subtract(config.getMaxDrawdown())) < 0;
    }

    public Map<Signal.Strategy, BigDecimal> getStrategyPerformance() {
        List<Position> closedPositions = positionRepository.findByIsOpen(false);

        return closedPositions.stream()
                .collect(Collectors.groupingBy(
                        Position::getStrategy,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Position::getProfit,
                                BigDecimal::add
                        )
                ));
    }

    public void rebalancePortfolio() {
        Map<Signal.Strategy, BigDecimal> performance = getStrategyPerformance();

        // Log performance for monitoring
        performance.forEach((strategy, profit) ->
                log.info("Strategy {} performance: {}", strategy, profit)
        );

        // Implement dynamic rebalancing logic based on performance
        // This could adjust strategy allocations based on recent performance
    }
}
