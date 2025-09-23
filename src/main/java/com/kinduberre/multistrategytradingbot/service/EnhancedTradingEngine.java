package com.kinduberre.multistrategytradingbot.service;


import com.kinduberre.multistrategytradingbot.model.Alert;
import com.kinduberre.multistrategytradingbot.model.MarketData;
import com.kinduberre.multistrategytradingbot.model.SystemHealth;
import com.kinduberre.multistrategytradingbot.model.TradingMetrics;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedTradingEngine implements BinanceWebSocketService.MarketDataListener {

    private final TradingEngine tradingEngine;
    private final AlertService alertService;
    private final DashboardService dashboardService;
    private final TradingMetrics metrics;
    private final BinanceWebSocketService webSocketService;

    @PostConstruct
    public void initialize() {
        // Subscribe to WebSocket data
        webSocketService.setMarketDataListener(this);

        // Send startup alert
        Alert alert = new Alert();
        alert.setSeverity(Alert.Severity.INFO);
        alert.setType(Alert.Type.SYSTEM_ERROR);
        alert.setMessage("Trading bot started");
        alert.setDetails("All systems initialized");
        alert.setTimestamp(LocalDateTime.now());
        alertService.sendAlert(alert);
    }

    @Override
    public void onMarketData(MarketData data) {
        // Process real-time data
        processRealTimeData(data);
    }

    private void processRealTimeData(MarketData data) {
        try {
            // Quick checks on real-time data
            checkCriticalLevels(data);

            // Update dashboard in real-time
            dashboardService.recordEquityPoint();

        } catch (Exception e) {
            log.error("Error processing real-time data", e);
        }
    }

    private void checkCriticalLevels(MarketData data) {
        // Check for sudden price movements
        BigDecimal priceChange = calculatePriceChange(data);

        if (priceChange.abs().compareTo(BigDecimal.valueOf(5)) > 0) {
            Alert alert = new Alert();
            alert.setSeverity(Alert.Severity.WARNING);
            alert.setType(Alert.Type.SIGNAL_GENERATED);
            alert.setMessage("Large price movement detected");
            alert.setDetails(String.format("Price changed by %.2f%%", priceChange));
            alert.setTimestamp(LocalDateTime.now());
            alertService.sendAlert(alert);
        }
    }

    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void performSystemCheck() {
        SystemHealth health = dashboardService.getSystemHealth();

        if (!health.getWarnings().isEmpty()) {
            Alert alert = new Alert();
            alert.setSeverity(Alert.Severity.WARNING);
            alert.setType(Alert.Type.SYSTEM_ERROR);
            alert.setMessage("System health check failed");
            alert.setDetails(String.join(", ", health.getWarnings()));
            alert.setTimestamp(LocalDateTime.now());
            alertService.sendAlert(alert);
        }
    }

    private BigDecimal calculatePriceChange(MarketData data) {
        // Implementation to calculate price change percentage
        return BigDecimal.ZERO;
    }
}