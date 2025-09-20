package com.kinduberre.multistrategytradingbot.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StrategyPerformance {
    private int trades;
    private BigDecimal totalPnL;
    private BigDecimal winRate;
    private BigDecimal averageReturn;
}