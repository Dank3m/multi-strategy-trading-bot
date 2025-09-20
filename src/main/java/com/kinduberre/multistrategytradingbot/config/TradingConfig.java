package com.kinduberre.multistrategytradingbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "trading")
@Data
public class TradingConfig {
    private BigDecimal accountBalance = new BigDecimal("10000");
    private BigDecimal riskPerTrade = new BigDecimal("0.01"); // 1% per trade
    private BigDecimal maxDrawdown = new BigDecimal("0.15"); // 15% max drawdown

    private StrategyAllocation allocation = new StrategyAllocation();

    @Data
    public static class StrategyAllocation {
        private BigDecimal trendFollowing = new BigDecimal("0.40");
        private BigDecimal volatilityBreakout = new BigDecimal("0.30");
        private BigDecimal rangeReversion = new BigDecimal("0.20");
        private BigDecimal volumeSpikeReversal = new BigDecimal("0.10");
    }

    private IndicatorSettings indicators = new IndicatorSettings();

    @Data
    public static class IndicatorSettings {
        private int shortSma = 20;
        private int mediumSma = 50;
        private int longSma = 200;
        private int atrPeriod = 14;
        private int bbPeriod = 20;
        private int rsiPeriod = 14;
        private BigDecimal atrMultiplier = new BigDecimal("1.5");
        private BigDecimal volumeMultiplier = new BigDecimal("1.5");
    }
}