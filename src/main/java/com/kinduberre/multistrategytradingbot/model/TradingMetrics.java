package com.kinduberre.multistrategytradingbot.model;

import io.micrometer.core.instrument.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class TradingMetrics {

    private final MeterRegistry meterRegistry;

    private Counter tradesOpened;
    private Counter tradesClosed;
    private Counter signalsGenerated;
    private Gauge openPositions;
    private Gauge totalPnL;
    private Gauge accountBalance;
    private Timer orderExecutionTime;
    private Counter alertsSent;

    @PostConstruct
    public void init() {
        tradesOpened = Counter.builder("trading.trades.opened")
                .description("Total number of trades opened")
                .register(meterRegistry);

        tradesClosed = Counter.builder("trading.trades.closed")
                .description("Total number of trades closed")
                .register(meterRegistry);

        signalsGenerated = Counter.builder("trading.signals.generated")
                .description("Total number of trading signals generated")
                .tag("type", "all")
                .register(meterRegistry);

        alertsSent = Counter.builder("trading.alerts.sent")
                .description("Total number of alerts sent")
                .register(meterRegistry);

        orderExecutionTime = Timer.builder("trading.order.execution.time")
                .description("Time taken to execute orders")
                .register(meterRegistry);
    }

    public void recordTradeOpened() {
        tradesOpened.increment();
    }

    public void recordTradeClosed() {
        tradesClosed.increment();
    }

    public void recordSignalGenerated(String strategyName) {
        Counter.builder("trading.signals.generated")
                .tag("strategy", strategyName)
                .register(meterRegistry)
                .increment();
    }

    public void recordAlertSent(String alertType) {
        Counter.builder("trading.alerts.sent")
                .tag("type", alertType)
                .register(meterRegistry)
                .increment();
    }

    public void recordOrderExecutionTime(long milliseconds) {
        orderExecutionTime.record(milliseconds, TimeUnit.MILLISECONDS);
    }

    public void updateGauges(int openPositionCount, BigDecimal pnl, BigDecimal balance) {
        Gauge.builder("trading.positions.open", openPositionCount, Integer::doubleValue)
                .register(meterRegistry);

        Gauge.builder("trading.pnl.total", pnl, BigDecimal::doubleValue)
                .register(meterRegistry);

        Gauge.builder("trading.account.balance", balance, BigDecimal::doubleValue)
                .register(meterRegistry);
    }
}