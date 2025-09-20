package com.kinduberre.multistrategytradingbot.model;

import lombok.Data;

@Data
public class HistoricalTrade {
    private double[] features;
    private String outcome; // PROFITABLE, LOSS, BREAKEVEN
}