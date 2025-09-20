package com.kinduberre.multistrategytradingbot.controller;

import com.kinduberre.multistrategytradingbot.model.MarketData;
import com.kinduberre.multistrategytradingbot.model.Position;
import com.kinduberre.multistrategytradingbot.service.BinanceService;
import com.kinduberre.multistrategytradingbot.service.PositionManagementService;
import com.kinduberre.multistrategytradingbot.service.RiskManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/trading")
@RequiredArgsConstructor
public class TradingController {
    private final PositionManagementService positionService;
    private final BinanceService binanceService;
    private final RiskManagementService riskService;

    @GetMapping("/positions/open")
    public List<Position> getOpenPositions() {
        return positionService.getOpenPositions();
    }

    @GetMapping("/pnl/total")
    public BigDecimal getTotalPnL() {
        return positionService.getTotalPnL();
    }

    @GetMapping("/balance/{asset}")
    public BigDecimal getBalance(@PathVariable String asset) {
        return binanceService.getAccountBalance(asset);
    }

    @GetMapping("/exposure")
    public BigDecimal getCurrentExposure() {
        return riskService.getCurrentExposure();
    }

    @PostMapping("/position/close/{id}")
    public Position closePosition(@PathVariable Long id, @RequestParam BigDecimal price) {
        return positionService.closePosition(id, price);
    }

    @GetMapping("/market/{symbol}")
    public List<MarketData> getMarketData(@PathVariable String symbol) {
        return binanceService.getKlines(symbol, "1h", 100);
    }
}
