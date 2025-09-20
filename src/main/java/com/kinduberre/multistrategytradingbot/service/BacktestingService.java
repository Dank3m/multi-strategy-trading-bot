package com.kinduberre.multistrategytradingbot.service;

import com.kinduberre.multistrategytradingbot.config.BacktestConfig;
import com.kinduberre.multistrategytradingbot.model.BacktestResult;
import com.kinduberre.multistrategytradingbot.model.BacktestTrade;
import com.kinduberre.multistrategytradingbot.model.MarketData;
import com.kinduberre.multistrategytradingbot.model.Signal;
import com.kinduberre.multistrategytradingbot.strategy.impl.RangeMeanReversionStrategy;
import com.kinduberre.multistrategytradingbot.strategy.impl.TrendFollowingStrategy;
import com.kinduberre.multistrategytradingbot.strategy.impl.VolatilityBreakoutStrategy;
import com.kinduberre.multistrategytradingbot.strategy.impl.VolumeSpikeReversalStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestingService {
    private final TrendFollowingStrategy trendFollowing;
    private final VolatilityBreakoutStrategy volatilityBreakout;
    private final RangeMeanReversionStrategy meanReversion;
    private final VolumeSpikeReversalStrategy volumeSpike;

    public BacktestResult runBacktest(List<MarketData> historicalData,
                                      BigDecimal initialCapital,
                                      BacktestConfig config) {

        log.info("Starting backtest with {} data points", historicalData.size());

        BacktestResult result = new BacktestResult();
        result.setInitialCapital(initialCapital);
        result.setTrades(new ArrayList<>());

        BigDecimal capital = initialCapital;
        List<BacktestTrade> openTrades = new ArrayList<>();
        List<BigDecimal> equityCurve = new ArrayList<>();
        BigDecimal maxEquity = initialCapital;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        // Process each data point
        for (int i = config.getLookbackPeriod(); i < historicalData.size(); i++) {
            List<MarketData> lookback = historicalData.subList(
                    i - config.getLookbackPeriod(), i + 1
            );

            MarketData current = historicalData.get(i);

            // Check stops for open trades
            checkStopsAndTargets(openTrades, current, result);

            // Generate signals from all strategies
            List<Signal> signals = generateSignals(lookback, config);

            // Process best signal
            Signal bestSignal = selectBestSignal(signals);

            if (bestSignal != null && bestSignal.getType() == Signal.SignalType.BUY) {
                if (openTrades.size() < config.getMaxPositions()) {
                    BacktestTrade trade = openPosition(
                            bestSignal, current, capital, config
                    );
                    if (trade != null) {
                        openTrades.add(trade);
                        capital = capital.subtract(
                                trade.getEntryPrice().multiply(trade.getQuantity())
                        );
                    }
                }
            } else if (bestSignal != null && bestSignal.getType() == Signal.SignalType.SELL) {
                closeMatchingPositions(openTrades, bestSignal, current, result);
            }

            // Calculate equity and drawdown
            BigDecimal currentEquity = calculateEquity(capital, openTrades, current);
            equityCurve.add(currentEquity);

            if (currentEquity.compareTo(maxEquity) > 0) {
                maxEquity = currentEquity;
            }

            BigDecimal drawdown = maxEquity.subtract(currentEquity)
                    .divide(maxEquity, 4, RoundingMode.HALF_UP);

            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        // Close all remaining positions
        MarketData lastData = historicalData.get(historicalData.size() - 1);
        for (BacktestTrade trade : openTrades) {
            closeTrade(trade, lastData.getClose(), lastData.getTimestamp(), "End of backtest");
            result.getTrades().add(trade);
        }

        // Calculate final metrics
        result.setFinalCapital(calculateEquity(capital, new ArrayList<>(), lastData));
        result.setTotalReturn(result.getFinalCapital().subtract(initialCapital));
        result.setTotalReturnPercent(
                result.getTotalReturn().divide(initialCapital, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
        );
        result.setMaxDrawdown(maxDrawdown.multiply(BigDecimal.valueOf(100)));

        calculateTradeStatistics(result);
        calculateSharpeRatio(result, equityCurve);
        calculateStrategyPerformance(result);
        calculateMonthlyReturns(result, historicalData);

        log.info("Backtest complete. Total return: {}%", result.getTotalReturnPercent());

        return result;
    }

    private List<Signal> generateSignals(List<MarketData> data, BacktestConfig config) {
        List<Signal> signals = new ArrayList<>();

        if (config.isEnableTrendFollowing()) {
            signals.add(trendFollowing.analyze(data));
        }
        if (config.isEnableVolatilityBreakout()) {
            signals.add(volatilityBreakout.analyze(data));
        }
        if (config.isEnableMeanReversion()) {
            signals.add(meanReversion.analyze(data));
        }
        if (config.isEnableVolumeSpike()) {
            signals.add(volumeSpike.analyze(data));
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

    private BacktestTrade openPosition(Signal signal, MarketData current,
                                       BigDecimal capital, BacktestConfig config) {
        BacktestTrade trade = new BacktestTrade();
        trade.setEntryTime(current.getTimestamp());
        trade.setEntryPrice(current.getClose());
        trade.setStrategy(signal.getStrategy());

        // Calculate position size
        BigDecimal riskAmount = capital.multiply(config.getRiskPerTrade());
        BigDecimal stopDistance = signal.getPrice().subtract(signal.getStopLoss()).abs();
        BigDecimal positionSize = riskAmount.divide(stopDistance, 8, RoundingMode.HALF_UP);

        // Apply commission
        BigDecimal commission = positionSize.multiply(current.getClose())
                .multiply(config.getCommission());

        trade.setQuantity(positionSize);

        return trade;
    }

    private void closeTrade(BacktestTrade trade, BigDecimal exitPrice,
                            LocalDateTime exitTime, String reason) {
        trade.setExitPrice(exitPrice);
        trade.setExitTime(exitTime);
        trade.setExitReason(reason);

        BigDecimal pnl = exitPrice.subtract(trade.getEntryPrice())
                .multiply(trade.getQuantity());
        trade.setPnl(pnl);
        trade.setPnlPercent(
                pnl.divide(trade.getEntryPrice().multiply(trade.getQuantity()),
                                4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
        );
    }

    private void calculateTradeStatistics(BacktestResult result) {
        List<BacktestTrade> trades = result.getTrades();
        result.setTotalTrades(trades.size());

        BigDecimal totalWin = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;
        int wins = 0;
        int losses = 0;

        for (BacktestTrade trade : trades) {
            if (trade.getPnl().compareTo(BigDecimal.ZERO) > 0) {
                wins++;
                totalWin = totalWin.add(trade.getPnl());
            } else {
                losses++;
                totalLoss = totalLoss.add(trade.getPnl().abs());
            }
        }

        result.setWinningTrades(wins);
        result.setLosingTrades(losses);

        if (wins > 0) {
            result.setAverageWin(totalWin.divide(BigDecimal.valueOf(wins),
                    4, RoundingMode.HALF_UP));
            result.setWinRate(BigDecimal.valueOf(wins)
                    .divide(BigDecimal.valueOf(trades.size()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }

        if (losses > 0) {
            result.setAverageLoss(totalLoss.divide(BigDecimal.valueOf(losses),
                    4, RoundingMode.HALF_UP));
        }

        if (totalLoss.compareTo(BigDecimal.ZERO) > 0) {
            result.setProfitFactor(totalWin.divide(totalLoss, 2, RoundingMode.HALF_UP));
        }
    }

    private void calculateSharpeRatio(BacktestResult result, List<BigDecimal> equityCurve) {
        if (equityCurve.size() < 2) return;

        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < equityCurve.size(); i++) {
            BigDecimal dailyReturn = equityCurve.get(i).subtract(equityCurve.get(i-1))
                    .divide(equityCurve.get(i-1), 8, RoundingMode.HALF_UP);
            returns.add(dailyReturn);
        }

        BigDecimal avgReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 8, RoundingMode.HALF_UP);

        BigDecimal variance = BigDecimal.ZERO;
        for (BigDecimal ret : returns) {
            BigDecimal diff = ret.subtract(avgReturn);
            variance = variance.add(diff.multiply(diff));
        }

        BigDecimal stdDev = BigDecimal.valueOf(
                Math.sqrt(variance.divide(BigDecimal.valueOf(returns.size()),
                        8, RoundingMode.HALF_UP).doubleValue())
        );

        if (stdDev.compareTo(BigDecimal.ZERO) > 0) {
            // Annualized Sharpe ratio (assuming daily data)
            BigDecimal sharpe = avgReturn.multiply(BigDecimal.valueOf(Math.sqrt(252)))
                    .divide(stdDev.multiply(BigDecimal.valueOf(Math.sqrt(252))),
                            2, RoundingMode.HALF_UP);
            result.setSharpeRatio(sharpe);
        }
    }

    private BigDecimal calculateEquity(BigDecimal cash, List<BacktestTrade> openTrades,
                                       MarketData current) {
        BigDecimal equity = cash;
        for (BacktestTrade trade : openTrades) {
            BigDecimal unrealizedPnL = current.getClose().subtract(trade.getEntryPrice())
                    .multiply(trade.getQuantity());
            equity = equity.add(unrealizedPnL);
        }
        return equity;
    }

    private void checkStopsAndTargets(List<BacktestTrade> openTrades, MarketData current,
                                      BacktestResult result) {
        // Implementation for checking stop losses and take profits
    }

    private void closeMatchingPositions(List<BacktestTrade> openTrades, Signal signal,
                                        MarketData current, BacktestResult result) {
        // Implementation for closing positions based on signals
    }

    private void calculateStrategyPerformance(BacktestResult result) {
        // Implementation for calculating per-strategy performance
    }

    private void calculateMonthlyReturns(BacktestResult result, List<MarketData> data) {
        // Implementation for calculating monthly returns
    }
}
