package com.kinduberre.multistrategytradingbot.service;

import com.kinduberre.multistrategytradingbot.model.MarketData;
import com.kinduberre.multistrategytradingbot.model.Position;
import com.kinduberre.multistrategytradingbot.model.Signal;
import com.kinduberre.multistrategytradingbot.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskManagementService {
    private final PositionRepository positionRepository;
    private final BinanceService binanceService;

    @Value("${trading.risk-per-trade}")
    private BigDecimal riskPerTrade;

    @Value("${trading.max-positions}")
    private int maxPositions;

    public BigDecimal calculatePositionSize(Signal signal, BigDecimal accountBalance) {
        if (signal.getStopLoss() == null) {
            log.warn("No stop loss defined for signal");
            return BigDecimal.ZERO;
        }

        // Calculate risk amount
        BigDecimal riskAmount = accountBalance.multiply(riskPerTrade);

        // Calculate risk per unit
        BigDecimal riskPerUnit = signal.getPrice().subtract(signal.getStopLoss()).abs();

        // Position size = Risk Amount / Risk per Unit
        BigDecimal positionSize = riskAmount.divide(riskPerUnit, 8, BigDecimal.ROUND_HALF_UP);

        // Check max position value (e.g., 10% of account)
        BigDecimal maxPositionValue = accountBalance.multiply(BigDecimal.valueOf(0.1));
        BigDecimal maxPositionSize = maxPositionValue.divide(signal.getPrice(), 8, BigDecimal.ROUND_HALF_UP);

        return positionSize.min(maxPositionSize);
    }

    public boolean canOpenPosition() {
        List<Position> openPositions = positionRepository.findByIsOpen(true);
        return openPositions.size() < maxPositions;
    }

    public BigDecimal getCurrentExposure() {
        List<Position> openPositions = positionRepository.findByIsOpen(true);
        return openPositions.stream()
                .map(p -> p.getQuantity().multiply(p.getEntryPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean validateSignal(Signal signal) {
        // Validate stop loss is present
        if (signal.getStopLoss() == null) {
            log.error("Signal rejected: No stop loss");
            return false;
        }

        // Validate risk/reward ratio (minimum 1:1.5)
        if (signal.getTakeProfit() != null) {
            BigDecimal risk = signal.getPrice().subtract(signal.getStopLoss()).abs();
            BigDecimal reward = signal.getTakeProfit().subtract(signal.getPrice()).abs();
            BigDecimal ratio = reward.divide(risk, 2, RoundingMode.HALF_UP);

            if (ratio.compareTo(BigDecimal.valueOf(1.5)) < 0) {
                log.warn("Signal has poor risk/reward ratio: {}", ratio);
                return false;
            }
        }

        // Check correlation with existing positions
        List<Position> openPositions = positionRepository.findByIsOpen(true);
        long sameStrategyCount = openPositions.stream()
                .filter(p -> p.getStrategy() == signal.getStrategy())
                .count();

        if (sameStrategyCount >= 2) {
            log.warn("Too many positions for strategy: {}", signal.getStrategy());
            return false;
        }

        return true;
    }

    public void updateTrailingStops(List<MarketData> marketData) {
        List<Position> openPositions = positionRepository.findByIsOpen(true);
        BigDecimal currentPrice = marketData.get(marketData.size() - 1).getClose();

        for (Position position : openPositions) {
            // Only trail stops for profitable positions
            if (currentPrice.compareTo(position.getEntryPrice()) > 0) {
                BigDecimal newStop = calculateTrailingStop(position, currentPrice, marketData);
                if (newStop.compareTo(position.getStopLoss()) > 0) {
                    position.setStopLoss(newStop);
                    positionRepository.save(position);
                    log.info("Updated trailing stop for position {} to {}",
                            position.getId(), newStop);
                }
            }
        }
    }

    private BigDecimal calculateTrailingStop(Position position, BigDecimal currentPrice,
                                             List<MarketData> marketData) {
        // Use ATR-based trailing stop
        TechnicalIndicatorService indicators = new TechnicalIndicatorService();
        BigDecimal atr = indicators.calculateATR(marketData, 14);

        if (atr == null) {
            return position.getStopLoss();
        }

        // Trail at 1.5 ATR below current price
        return currentPrice.subtract(atr.multiply(BigDecimal.valueOf(1.5)));
    }
}
