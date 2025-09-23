package com.kinduberre.multistrategytradingbot.service;


import com.kinduberre.multistrategytradingbot.model.*;
import com.kinduberre.multistrategytradingbot.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PositionRepository positionRepository;
    private final PositionManagementService positionService;
    private final BinanceService binanceService;
    private final RiskManagementService riskService;
    private final List<EquityPoint> equityHistory = new ArrayList<>();

    public DashboardMetrics getCurrentMetrics() {
        DashboardMetrics metrics = new DashboardMetrics();

        // Calculate P&L metrics
        metrics.setTotalPnL(positionService.getTotalPnL());
        metrics.setTodayPnL(calculateTodayPnL());

        BigDecimal currentPrice = getCurrentBTCPrice();
        metrics.setUnrealizedPnL(positionService.getUnrealizedPnL(currentPrice));

        // Trading statistics
        List<Position> allPositions = positionRepository.findAll();
        long wins = allPositions.stream()
                .filter(p -> p.getProfit() != null && p.getProfit().compareTo(BigDecimal.ZERO) > 0)
                .count();

        if (!allPositions.isEmpty()) {
            metrics.setWinRate(BigDecimal.valueOf(wins * 100.0 / allPositions.size()));
        }

        metrics.setTotalTrades(allPositions.size());
        metrics.setOpenPositions(positionService.getOpenPositions().size());

        // Account metrics
        metrics.setAccountBalance(binanceService.getAccountBalance("USDT"));
        metrics.setExposure(riskService.getCurrentExposure());

        // Performance metrics
        metrics.setSharpeRatio(calculateSharpeRatio());
        metrics.setMaxDrawdown(calculateMaxDrawdown());

        // Equity curve
        metrics.setEquityCurve(getEquityCurve(30));

        // Strategy breakdown
        metrics.setStrategyMetrics(getStrategyPerformance());

        // Recent trades
        metrics.setRecentTrades(getRecentTrades(10));

        // System health
        metrics.setSystemHealth(getSystemHealth());

        return metrics;
    }

    private BigDecimal calculateTodayPnL() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        List<Position> todayPositions = positionRepository.findAll().stream()
                .filter(p -> p.getExitTime() != null && p.getExitTime().isAfter(startOfDay))
                .collect(Collectors.toList());

        return todayPositions.stream()
                .map(Position::getProfit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getCurrentBTCPrice() {
        List<MarketData> klines = binanceService.getKlines("BTCUSDT", "1m", 1);
        return klines.isEmpty() ? BigDecimal.ZERO : klines.get(0).getClose();
    }

    private BigDecimal calculateSharpeRatio() {
        if (equityHistory.size() < 30) return BigDecimal.ZERO;

        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 1; i < equityHistory.size(); i++) {
            BigDecimal prevValue = equityHistory.get(i-1).getValue();
            BigDecimal currValue = equityHistory.get(i).getValue();

            if (prevValue.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal dailyReturn = currValue.subtract(prevValue)
                        .divide(prevValue, 8, BigDecimal.ROUND_HALF_UP);
                returns.add(dailyReturn);
            }
        }

        if (returns.isEmpty()) return BigDecimal.ZERO;

        // Calculate average return
        BigDecimal avgReturn = returns.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(returns.size()), 8, BigDecimal.ROUND_HALF_UP);

        // Calculate standard deviation
        BigDecimal variance = BigDecimal.ZERO;
        for (BigDecimal ret : returns) {
            BigDecimal diff = ret.subtract(avgReturn);
            variance = variance.add(diff.multiply(diff));
        }

        if (returns.size() > 1) {
            variance = variance.divide(BigDecimal.valueOf(returns.size() - 1),
                    8, BigDecimal.ROUND_HALF_UP);
        }

        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        if (stdDev.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        // Annualized Sharpe ratio
        return avgReturn.multiply(BigDecimal.valueOf(Math.sqrt(252)))
                .divide(stdDev.multiply(BigDecimal.valueOf(Math.sqrt(252))),
                        2, BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal calculateMaxDrawdown() {
        if (equityHistory.size() < 2) return BigDecimal.ZERO;

        BigDecimal maxEquity = BigDecimal.ZERO;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (EquityPoint point : equityHistory) {
            if (point.getValue().compareTo(maxEquity) > 0) {
                maxEquity = point.getValue();
            }

            if (maxEquity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal drawdown = maxEquity.subtract(point.getValue())
                        .divide(maxEquity, 4, BigDecimal.ROUND_HALF_UP);

                if (drawdown.compareTo(maxDrawdown) > 0) {
                    maxDrawdown = drawdown;
                }
            }
        }

        return maxDrawdown.multiply(BigDecimal.valueOf(100));
    }

    public List<EquityPoint> getEquityCurve(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        return equityHistory.stream()
                .filter(p -> p.getTimestamp().isAfter(startDate))
                .collect(Collectors.toList());
    }

    public Map<String, StrategyMetrics> getStrategyPerformance() {
        Map<String, StrategyMetrics> performance = new HashMap<>();

        List<Position> allPositions = positionRepository.findAll();

        Map<Signal.Strategy, List<Position>> strategyGroups = allPositions.stream()
                .filter(p -> p.getStrategy() != null)
                .collect(Collectors.groupingBy(Position::getStrategy));

        for (Map.Entry<Signal.Strategy, List<Position>> entry : strategyGroups.entrySet()) {
            StrategyMetrics metrics = new StrategyMetrics();
            metrics.setName(entry.getKey().name());

            List<Position> positions = entry.getValue();
            metrics.setTrades(positions.size());

            BigDecimal totalPnL = positions.stream()
                    .map(Position::getProfit)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            metrics.setPnl(totalPnL);

            long wins = positions.stream()
                    .filter(p -> p.getProfit() != null && p.getProfit().compareTo(BigDecimal.ZERO) > 0)
                    .count();

            if (!positions.isEmpty()) {
                metrics.setWinRate(BigDecimal.valueOf(wins * 100.0 / positions.size()));
                metrics.setAvgReturn(totalPnL.divide(BigDecimal.valueOf(positions.size()),
                        4, BigDecimal.ROUND_HALF_UP));
            }

            performance.put(entry.getKey().name(), metrics);
        }

        return performance;
    }

    private List<RecentTrade> getRecentTrades(int limit) {
        return positionRepository.findAll().stream()
                .filter(p -> p.getExitTime() != null)
                .sorted((a, b) -> b.getExitTime().compareTo(a.getExitTime()))
                .limit(limit)
                .map(this::convertToRecentTrade)
                .collect(Collectors.toList());
    }

    private RecentTrade convertToRecentTrade(Position position) {
        RecentTrade trade = new RecentTrade();
        trade.setTime(position.getExitTime());
        trade.setSymbol(position.getSymbol());
        trade.setSide("SELL"); // Exit trades
        trade.setPrice(position.getExitPrice());
        trade.setQuantity(position.getQuantity());
        trade.setPnl(position.getProfit());
        trade.setStrategy(position.getStrategy().name());
        return trade;
    }

    public SystemHealth getSystemHealth() {
        SystemHealth health = new SystemHealth();
        health.setTradingEnabled(true); // Check from config
        health.setDataFeedActive(true); // Check WebSocket status
        health.setAlertsActive(true); // Check alert service
        health.setApiCallsRemaining(1200); // Get from Binance headers
        health.setLastUpdate(LocalDateTime.now());

        List<String> warnings = new ArrayList<>();

        // Check for issues
        BigDecimal balance = binanceService.getAccountBalance("USDT");
        if (balance.compareTo(BigDecimal.valueOf(100)) < 0) {
            warnings.add("Low account balance");
        }

        BigDecimal drawdown = calculateMaxDrawdown();
        if (drawdown.compareTo(BigDecimal.valueOf(10)) > 0) {
            warnings.add("High drawdown detected");
        }

        health.setWarnings(warnings);

        return health;
    }

    public void recordEquityPoint() {
        BigDecimal balance = binanceService.getAccountBalance("USDT");
        BigDecimal unrealizedPnL = positionService.getUnrealizedPnL(getCurrentBTCPrice());

        EquityPoint point = new EquityPoint();
        point.setTimestamp(LocalDateTime.now());
        point.setValue(balance.add(unrealizedPnL));

        equityHistory.add(point);

        // Keep only last 90 days
        if (equityHistory.size() > 2160) { // 90 days * 24 hours
            equityHistory.remove(0);
        }
    }
}