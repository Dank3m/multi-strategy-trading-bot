package com.kinduberre.multistrategytradingbot.model;

import lombok.Data;

@Data
public class MLPrediction {
    private String action;
    private double confidence;
    private double buyProbability;
    private double sellProbability;
    private double holdProbability;
    private Signal originalSignal;
}