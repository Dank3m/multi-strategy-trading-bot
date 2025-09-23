package com.kinduberre.multistrategytradingbot.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EquityPoint {
    private LocalDateTime timestamp;
    private BigDecimal value;
}
