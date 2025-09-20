package com.kinduberre.multistrategytradingbot.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@Table(name = "positions")
@NoArgsConstructor
@AllArgsConstructor
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Signal.Strategy strategy;

    private String symbol;

    private BigDecimal entryPrice;
    private BigDecimal quantity;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private BigDecimal exitPrice;
    private BigDecimal profit;
    private boolean isOpen;
}