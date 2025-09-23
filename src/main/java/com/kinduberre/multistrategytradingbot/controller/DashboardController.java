package com.kinduberre.multistrategytradingbot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/metrics")
    public DashboardMetrics getMetrics() {
        return dashboardService.getCurrentMetrics();
    }

    @GetMapping("/equity-curve")
    public List<EquityPoint> getEquityCurve(@RequestParam(defaultValue = "30") int days) {
        return dashboardService.getEquityCurve(days);
    }

    @GetMapping("/strategy-performance")
    public Map<String, StrategyMetrics> getStrategyPerformance() {
        return dashboardService.getStrategyPerformance();
    }

    @GetMapping("/system-health")
    public SystemHealth getSystemHealth() {
        return dashboardService.getSystemHealth();
    }

    // Push real-time updates via WebSocket
    @Scheduled(fixedDelay = 5000)
    public void pushMetricsUpdate() {
        DashboardMetrics metrics = dashboardService.getCurrentMetrics();
        messagingTemplate.convertAndSend("/topic/metrics", metrics);
    }
}
