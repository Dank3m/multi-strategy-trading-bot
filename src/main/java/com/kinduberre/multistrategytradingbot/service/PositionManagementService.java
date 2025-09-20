package com.kinduberre.multistrategytradingbot.service;

import com.kinduberre.multistrategytradingbot.model.Position;
import com.kinduberre.multistrategytradingbot.model.Signal;
import com.kinduberre.multistrategytradingbot.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PositionManagementService {
    private final PositionRepository positionRepository;
    private final BinanceService binanceService;
    private final RiskManagementService riskManagementService;

    @Transactional
    public Position openPosition(Signal signal, BigDecimal quantity) {
        Position position = new Position();
        position.setSymbol("BTCUSDT");
        position.setEntryPrice(signal.getPrice());
        position.setQuantity(quantity);
        position.setStopLoss(signal.getStopLoss());
        position.setTakeProfit(signal.getTakeProfit());
        position.setStrategy(signal.getStrategy());
        position.setEntryTime(LocalDateTime.now());
        position.setOpen(true);

        Position saved = positionRepository.save(position);
        log.info("Opened position: {}", saved);

        // Place actual order on Binance
        String orderId = binanceService.placeOrder(
                position.getSymbol(),
                "BUY",
                quantity,
                signal.getPrice()
        );

        if (orderId != null) {
            log.info("Binance order placed: {}", orderId);
        }

        return saved;
    }

    @Transactional
    public Position closePosition(Long positionId, BigDecimal exitPrice) {
        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new RuntimeException("Position not found"));

        position.setExitPrice(exitPrice);
        position.setExitTime(LocalDateTime.now());
        position.setOpen(false);

        // Calculate PnL
        BigDecimal pnl = exitPrice.subtract(position.getEntryPrice())
                .multiply(position.getQuantity());
        position.setProfit(pnl);

        Position saved = positionRepository.save(position);
        log.info("Closed position: {} with PnL: {}", saved.getId(), pnl);

        // Place sell order on Binance
        String orderId = binanceService.placeOrder(
                position.getSymbol(),
                "SELL",
                position.getQuantity(),
                exitPrice
        );

        if (orderId != null) {
            log.info("Binance sell order placed: {}", orderId);
        }

        return saved;
    }

    public void checkStopLossAndTakeProfit(BigDecimal currentPrice) {
        List<Position> openPositions = positionRepository.findByIsOpen(true);

        for (Position position : openPositions) {
            // Check stop loss
            if (currentPrice.compareTo(position.getStopLoss()) <= 0) {
                log.warn("Stop loss triggered for position {}", position.getId());
                closePosition(position.getId(), currentPrice);
            }
            // Check take profit
            else if (position.getTakeProfit() != null &&
                    currentPrice.compareTo(position.getTakeProfit()) >= 0) {
                log.info("Take profit triggered for position {}", position.getId());
                closePosition(position.getId(), currentPrice);
            }
        }
    }

    public List<Position> getOpenPositions() {
        return positionRepository.findByIsOpen(true);
    }

    public BigDecimal getTotalPnL() {
        List<Position> closedPositions = positionRepository.findByIsOpen(false);
        return closedPositions.stream()
                .map(Position::getProfit)
                .filter(pnl -> pnl != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getUnrealizedPnL(BigDecimal currentPrice) {
        List<Position> openPositions = positionRepository.findByIsOpen(true);
        return openPositions.stream()
                .map(p -> currentPrice.subtract(p.getEntryPrice()).multiply(p.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
