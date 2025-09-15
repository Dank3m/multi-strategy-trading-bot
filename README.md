# Spring Boot Cryptocurrency Trading Bot

A comprehensive automated trading bot for cryptocurrency markets with multiple strategies and backtesting capabilities.

## Features

### Trading Strategies
1. **Trend Following Strategy** (40% allocation)
   - Uses 50/200 SMA crossover with volume confirmation
   - Breakout detection with trailing stop loss
   - Best for trending markets

2. **Volatility Breakout Strategy** (30% allocation)
   - Identifies compression periods using ATR
   - Trades breakouts with volume confirmation
   - Risk/reward ratio optimization

3. **Mean Reversion Strategy** (20% allocation)
   - Bollinger Bands for range detection
   - RSI for overbought/oversold conditions
   - Works best in sideways markets

4. **Volume Spike Reversal Strategy** (10% allocation)
   - Detects exhaustion patterns
   - Long wicks with extreme volume
   - Short-term reversal trades

5. **Volume-Weighted Breakout Strategy**
   - Uses USDT volume for confirmation
   - Superior signal quality with money flow analysis

### Risk Management
- Position sizing using Kelly Criterion
- ATR-based stop losses
- Maximum drawdown limits (20%)
- Portfolio exposure limits (50%)
- Correlation management
- News/anomaly detection via trade count

## Installation

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Optional: Docker for containerized deployment

### Build
```bash
git clone <repository>
cd multi-strategy-trading-bot
mvn clean install
```

### Configuration
Edit `application.yml`:

```yaml
trading:
  symbol: BTCUSDT
  account:
    balance: 10000
  risk:
    per-trade: 0.01  # 1% risk per trade
    max-drawdown: 0.2  # 20% max drawdown
  strategy:
    trend-allocation: 0.4
    volatility-allocation: 0.3
    reversion-allocation: 0.2
    spike-allocation: 0.1
```

## Running the Application

### Start the Trading Bot
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### API Endpoints

#### Trading Control
- `POST /api/trading/start` - Start automated trading
- `POST /api/trading/stop` - Stop automated trading
- `GET /api/trading/status` - Get current status and positions
- `GET /api/trading/positions` - List all open positions

#### Backtesting

##### Run Backtest with CSV File
```bash
curl -X POST http://localhost:8080/api/backtest/run \
  -F "file=@your_data.csv" \
  -F "initialBalance=10000" \
  -F "riskPerTrade=0.01"
```

##### CSV Format Required
```csv
Unix,Date,Symbol,Open,High,Low,Close,Volume BTC,Volume USDT,tradecount
1757289600000,2025-09-08,BTCUSDT,111137.35,112924.37,110621.78,112065.23,11582.40211,1295903862.4649603,1744598
```

##### Compare Strategies
```bash
curl -X POST http://localhost:8080/api/backtest/compare \
  -F "file=@your_data.csv"
```

##### Get Backtest Report
```bash
curl http://localhost:8080/api/backtest/report/{reportId}
```

## Backtesting Guide

### Running a Backtest Programmatically

```java
@Autowired
private BacktestEngine backtestEngine;

public void runBacktest() {
    BacktestConfig config = new BacktestConfig();
    config.setInitialBalance(BigDecimal.valueOf(10000));
    config.setRiskPerTrade(BigDecimal.valueOf(0.01));
    
    BacktestResults results = backtestEngine.runBacktest(
        "path/to/your/data.csv", 
        config
    );
    
    System.out.println("Total Return: " + results.getTotalReturnPercent() + "%");
    System.out.println("Sharpe Ratio: " + results.getSharpeRatio());
    System.out.println("Max Drawdown: " + results.getMaxDrawdown() + "%");
}
```

### Backtest Results Include:
- **Performance Metrics**: Total return, Sharpe ratio, max drawdown
- **Trade Statistics**: Win rate, profit factor, average win/loss
- **Strategy Breakdown**: Individual strategy performance
- **Equity Curve**: Balance over time with drawdown tracking
- **Trade List**: Detailed entry/exit information
- **Monthly Returns**: Month-by-month performance

### Reports Generated
1. **JSON Report**: Complete backtest results in `reports/backtest_*.json`
2. **CSV Trades**: All trades in `reports/trades_*.csv`
3. **API Response**: Real-time results via REST endpoint

## Live Trading Setup

### Connect to Binance (Example)
1. Add API credentials to `application.yml`:
```yaml
binance:
  api:
    key: your-api-key
    secret: your-api-secret
```

2. The bot will automatically:
   - Fetch live market data every minute
   - Analyze signals across all strategies
   - Execute trades based on risk management rules
   - Update positions and trailing stops
   - Log all activities

## Monitoring

### Metrics Endpoint
- `GET /actuator/health` - Application health
- `GET /actuator/metrics` - Performance metrics
- `GET /actuator/prometheus` - Prometheus format metrics

### Swagger UI
Access API documentation at: `http://localhost:8080/swagger-ui.html`

## Docker Deployment

### Build Docker Image
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/spring-trading-bot-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker build -t trading-bot .
docker run -p 8080:8080 trading-bot
```

## Testing

### Run Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

## Safety Features

1. **Maximum Position Limits**: Each strategy has position count limits
2. **Drawdown Protection**: Stops trading at 20% drawdown
3. **Anomaly Detection**: Reduces size on unusual trade counts
4. **Slippage Accounting**: Realistic execution prices
5. **Fee Calculation**: Maker/taker fees included

## Performance Optimization

### Recommended Settings
- **Trend Markets**: Increase trend-following allocation
- **Choppy Markets**: Increase mean-reversion allocation
- **High Volatility**: Reduce position sizes
- **Low Volatility**: Focus on breakout strategies

### Backtesting Best Practices
1. Use at least 200 days of data for meaningful results
2. Account for fees and slippage
3. Test across different market conditions
4. Monitor strategy correlation
5. Regular rebalancing of allocations

## Troubleshooting

### Common Issues

1. **Insufficient Data Error**
   - Ensure CSV has at least 200 rows
   - Check date formatting

2. **No Trades Executed**
   - Verify risk parameters aren't too conservative
   - Check if strategies are enabled
   - Review market conditions

3. **High Drawdown**
   - Reduce position sizes
   - Adjust stop-loss distances
   - Review strategy allocations

## License
MIT License

## Support
For issues and questions, please open a GitHub issue.

## Disclaimer
This bot is for educational purposes. Always test thoroughly with paper trading before using real funds. Cryptocurrency trading carries significant risk.
