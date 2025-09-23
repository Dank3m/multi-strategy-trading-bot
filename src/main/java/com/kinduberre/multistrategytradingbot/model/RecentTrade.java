package com.kinduberre.multistrategytradingbot.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RecentTrade {
    private LocalDateTime time;
    private String symbol;
    private String side;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal pnl;
    private String strategy;
}
