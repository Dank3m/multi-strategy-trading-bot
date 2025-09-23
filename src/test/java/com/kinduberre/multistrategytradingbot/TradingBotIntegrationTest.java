package com.kinduberre.multistrategytradingbot;

import com.kinduberre.multistrategytradingbot.config.BacktestConfig;
import com.kinduberre.multistrategytradingbot.model.*;
import com.kinduberre.multistrategytradingbot.service.*;
import com.kinduberre.multistrategytradingbot.strategy.impl.TrendFollowingStrategy;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TradingBotIntegrationTest {

    @Mock
    private BinanceService binanceService;

    @InjectMocks
    private TradingEngine tradingEngine;

    @Mock
    private BacktestingService backtestingService;

    @Mock
    private RiskManagementService riskManagementService;

    @Test
    void testTrendFollowingStrategy() {
        // Prepare test data
        List<MarketData> testData = generateTestData();

        TrendFollowingStrategy strategy = new TrendFollowingStrategy(new TechnicalIndicatorService());
        Signal signal = strategy.analyze(testData);

        assertNotNull(signal);
        assertTrue(signal.getConfidence() > 0);
    }

    @Test
    void testRiskManagement() {

        Signal signal = new Signal();
        signal.setPrice(BigDecimal.valueOf(50000));
        signal.setStopLoss(BigDecimal.valueOf(49000));

        BigDecimal positionSize = riskManagementService.calculatePositionSize(
                signal, BigDecimal.valueOf(10000)
        );

        // Should risk 1% = $100, with $1000 risk per coin = 0.1 BTC
        assertEquals(0, positionSize.compareTo(BigDecimal.valueOf(0.1)));
    }

    @Test
    void testBacktesting() {

        List<MarketData> historicalData = loadHistoricalData();

        BacktestConfig config = new BacktestConfig();
        BacktestResult result = backtestingService.runBacktest(
                historicalData,
                BigDecimal.valueOf(10000),
                config
        );

        assertNotNull(result);
        assertTrue(result.getTotalTrades() > 0);
        assertNotNull(result.getSharpeRatio());
    }

    @Test
    void testMLSignalFilter() {
        MLSignalFilter mlFilter = new MLSignalFilter();

        Signal signal = new Signal();
        signal.setType(Signal.SignalType.BUY);

        MarketFeatures features = new MarketFeatures();
        features.setRsi(65);
        features.setVolumeRatio(1.5);

        MLPrediction prediction = mlFilter.filterSignal(signal, features);

        assertNotNull(prediction);
        assertTrue(prediction.getConfidence() >= 0 && prediction.getConfidence() <= 1);
    }

    private List<MarketData> generateTestData() {
        // Generate test market data
        List<MarketData> data = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            MarketData md = new MarketData();
            md.setClose(BigDecimal.valueOf(50000 + i * 100));
            md.setVolume(BigDecimal.valueOf(1000));
            data.add(md);
        }
        return data;
    }

    private List<MarketData> loadHistoricalData() {
        // Load historical data for backtesting
        return generateTestData();
    }
}