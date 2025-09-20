package com.kinduberre.multistrategytradingbot.service;

import com.kinduberre.multistrategytradingbot.model.MarketData;
import com.kinduberre.multistrategytradingbot.model.Position;
import com.kinduberre.multistrategytradingbot.model.Signal;
import com.kinduberre.multistrategytradingbot.strategy.impl.RangeMeanReversionStrategy;
import com.kinduberre.multistrategytradingbot.strategy.impl.TrendFollowingStrategy;
import com.kinduberre.multistrategytradingbot.strategy.impl.VolatilityBreakoutStrategy;
import com.kinduberre.multistrategytradingbot.strategy.impl.VolumeSpikeReversalStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradingEngine {

    private final BinanceService binanceService;
    private final RiskManagementService riskManagementService;
    private final PositionManagementService positionManagementService;
    private final TrendFollowingStrategy trendFollowingStrategy;
    private final VolatilityBreakoutStrategy volatilityBreakoutStrategy;
    private final RangeMeanReversionStrategy meanReversionStrategy;
    private final VolumeSpikeReversalStrategy volumeSpikeReversalStrategy;

    @Value("${trading.symbol}")
    private String symbol;

    @Value("${trading.enable-live-trading}")
    private boolean enableLiveTrading;

    @Value("${trading.strategies.trend-following.enabled}")
    private boolean trendFollowingEnabled;

    @Value("${trading.strategies.volatility-breakout.enabled}")
    private boolean volatilityBreakoutEnabled;

    @Value("${trading.strategies.mean-reversion.enabled}")
    private boolean meanReversionEnabled;

    @Value("${trading.strategies.volume-spike.enabled}")
    private boolean volumeSpikeEnabled;

    @Scheduled(fixedDelay = 60000) // Run every minute
    public void executeTradingCycle() {
        try {
            log.info("Starting trading cycle for {}", symbol);

            // Fetch market data
            List<MarketData> marketData = binanceService.getKlines(symbol, "1h", 200);

            if (marketData.isEmpty()) {
                log.error("No market data available");
                return;
            }

            BigDecimal currentPrice = marketData.get(marketData.size() - 1).getClose();

            // Check existing positions for stop loss and take profit
            positionManagementService.checkStopLossAndTakeProfit(currentPrice);

            // Update trailing stops
            riskManagementService.updateTrailingStops(marketData);

            // Analyze signals from all strategies
            List<Signal> signals = analyzeStrategies(marketData);

            // Filter and rank signals
            Signal bestSignal = selectBestSignal(signals);

            if (bestSignal != null && bestSignal.getType() != Signal.SignalType.HOLD) {
                processSignal(bestSignal, currentPrice);
            }

            // Log current status
            logTradingStatus(currentPrice);

        } catch (Exception e) {
            log.error("Error in trading cycle: ", e);
        }
    }

    private List<Signal> analyzeStrategies(List<MarketData> marketData) {
        List<Signal> signals = new ArrayList<>();

        if (trendFollowingEnabled) {
            signals.add(trendFollowingStrategy.analyze(marketData));
        }

        if (volatilityBreakoutEnabled) {
            signals.add(volatilityBreakoutStrategy.analyze(marketData));
        }

        if (meanReversionEnabled) {
            signals.add(meanReversionStrategy.analyze(marketData));
        }

        if (volumeSpikeEnabled) {
            signals.add(volumeSpikeReversalStrategy.analyze(marketData));
        }

        return signals;
    }

    private Signal selectBestSignal(List<Signal> signals) {
        return signals.stream()
                .filter(s -> s.getType() != Signal.SignalType.HOLD)
                .filter(s -> s.getConfidence() != null)
                .max(Comparator.comparing(Signal::getConfidence))
                .orElse(null);
    }

    private void processSignal(Signal signal, BigDecimal currentPrice) {
        log.info("Processing signal: {} from {}", signal.getType(), signal.getStrategy());

        if (signal.getType() == Signal.SignalType.BUY) {
            if (!riskManagementService.canOpenPosition()) {
                log.warn("Cannot open position - max positions reached");
                return;
            }

            if (!riskManagementService.validateSignal(signal)) {
                log.warn("Signal failed validation");
                return;
            }

            BigDecimal accountBalance = binanceService.getAccountBalance("USDT");
            BigDecimal positionSize = riskManagementService.calculatePositionSize(signal, accountBalance);

            if (positionSize.compareTo(BigDecimal.ZERO) > 0) {
                if (enableLiveTrading) {
                    positionManagementService.openPosition(signal, positionSize);
                } else {
                    log.info("SIMULATION: Would open position with size {}", positionSize);
                }
            }
        } else if (signal.getType() == Signal.SignalType.SELL) {
            // Check if we have positions to close
            List<Position> openPositions = positionManagementService.getOpenPositions();

            for (Position position : openPositions) {
                if (position.getStrategy() == signal.getStrategy()) {
                    if (enableLiveTrading) {
                        positionManagementService.closePosition(position.getId(), currentPrice);
                    } else {
                        log.info("SIMULATION: Would close position {}", position.getId());
                    }
                }
            }
        }
    }

    private void logTradingStatus(BigDecimal currentPrice) {
        List<Position> openPositions = positionManagementService.getOpenPositions();
        BigDecimal totalPnL = positionManagementService.getTotalPnL();
        BigDecimal unrealizedPnL = positionManagementService.getUnrealizedPnL(currentPrice);

        log.info("=== Trading Status ===");
        log.info("Current Price: {}", currentPrice);
        log.info("Open Positions: {}", openPositions.size());
        log.info("Total Realized PnL: {}", totalPnL);
        log.info("Unrealized PnL: {}", unrealizedPnL);
        log.info("====================");
    }
}
