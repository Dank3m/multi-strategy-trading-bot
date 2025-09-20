package com.kinduberre.multistrategytradingbot.model;

import lombok.Data;

@Data
public class MarketFeatures {
    private double rsi;
    private double macdSignal;
    private double bollingerPosition;
    private double volumeRatio;
    private double priceChange1h;
    private double priceChange24h;
    private double atrRatio;
    private double trendStrength;
    private double support;
    private double resistance;
    // ... add more features

    public double[] toArray() {
        return new double[] {
                rsi, macdSignal, bollingerPosition, volumeRatio,
                priceChange1h, priceChange24h, atrRatio, trendStrength,
                support, resistance
        };
    }
}