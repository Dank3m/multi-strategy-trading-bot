package com.kinduberre.multistrategytradingbot.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BacktestTrade {
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private BigDecimal entryPrice;
    private BigDecimal exitPrice;
    private BigDecimal quantity;
    private BigDecimal pnl;
    private BigDecimal pnlPercent;
    private Signal.Strategy strategy;
    private String exitReason;
}
