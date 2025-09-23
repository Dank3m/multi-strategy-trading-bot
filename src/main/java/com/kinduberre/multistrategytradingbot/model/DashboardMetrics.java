package com.kinduberre.multistrategytradingbot.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DashboardMetrics {
    private BigDecimal totalPnL;
    private BigDecimal todayPnL;
    private BigDecimal unrealizedPnL;
    private BigDecimal winRate;
    private int totalTrades;
    private int openPositions;
    private BigDecimal accountBalance;
    private BigDecimal exposure;
    private BigDecimal sharpeRatio;
    private BigDecimal maxDrawdown;
    private List<EquityPoint> equityCurve;
    private Map<String, StrategyMetrics> strategyMetrics;
    private List<RecentTrade> recentTrades;
    private SystemHealth systemHealth;
}
