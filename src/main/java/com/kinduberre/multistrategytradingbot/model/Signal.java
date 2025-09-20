package com.kinduberre.multistrategytradingbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Signal {
    public enum SignalType { BUY, SELL, HOLD }
    public enum Strategy {
        TREND_FOLLOWING,
        VOLATILITY_BREAKOUT,
        MEAN_REVERSION,
        VOLUME_SPIKE_REVERSAL,
        VOLUME_WEIGHTED_BREAKOUT
    }

    private SignalType type;
    private Strategy strategy;
    private BigDecimal price;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private Double confidence;
    private LocalDateTime timestamp;
    private String reason;
}