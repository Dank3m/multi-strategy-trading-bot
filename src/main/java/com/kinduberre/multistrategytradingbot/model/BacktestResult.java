package com.kinduberre.multistrategytradingbot.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class BacktestResult {
    private BigDecimal initialCapital;
    private BigDecimal finalCapital;
    private BigDecimal totalReturn;
    private BigDecimal totalReturnPercent;
    private BigDecimal maxDrawdown;
    private BigDecimal sharpeRatio;
    private BigDecimal winRate;
    private int totalTrades;
    private int winningTrades;
    private int losingTrades;
    private BigDecimal averageWin;
    private BigDecimal averageLoss;
    private BigDecimal profitFactor;
    private List<BacktestTrade> trades;
    private Map<String, BigDecimal> monthlyReturns;
    private Map<Signal.Strategy, StrategyPerformance> strategyPerformance;
}
