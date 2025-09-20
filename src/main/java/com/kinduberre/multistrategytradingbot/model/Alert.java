package com.kinduberre.multistrategytradingbot.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Alert {
    public enum Severity { INFO, WARNING, CRITICAL }
    public enum Type {
        POSITION_OPENED, POSITION_CLOSED,
        STOP_LOSS_HIT, TAKE_PROFIT_HIT,
        LARGE_DRAWDOWN, SYSTEM_ERROR,
        SIGNAL_GENERATED, ML_PREDICTION
    }

    private Severity severity;
    private Type type;
    private String message;
    private String details;
    private LocalDateTime timestamp;
}
