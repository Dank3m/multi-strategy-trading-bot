package com.kinduberre.multistrategytradingbot.config;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BacktestConfig {
    private int lookbackPeriod = 200;
    private int maxPositions = 5;
    private BigDecimal riskPerTrade = BigDecimal.valueOf(0.01);
    private BigDecimal commission = BigDecimal.valueOf(0.001);
    private BigDecimal slippage = BigDecimal.valueOf(0.0005);
    private boolean enableTrendFollowing = true;
    private boolean enableVolatilityBreakout = true;
    private boolean enableMeanReversion = true;
    private boolean enableVolumeSpike = true;
}