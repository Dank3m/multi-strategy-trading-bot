package com.kinduberre.multistrategytradingbot.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StrategyMetrics {
    private String name;
    private int trades;
    private BigDecimal pnl;
    private BigDecimal winRate;
    private BigDecimal avgReturn;
}