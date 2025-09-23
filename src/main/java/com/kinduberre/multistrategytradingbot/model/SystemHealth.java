package com.kinduberre.multistrategytradingbot.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SystemHealth {
    private boolean tradingEnabled;
    private boolean dataFeedActive;
    private boolean alertsActive;
    private int apiCallsRemaining;
    private LocalDateTime lastUpdate;
    private List<String> warnings;
}
