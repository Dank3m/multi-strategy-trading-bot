package com.kinduberre.multistrategytradingbot.repository;

import com.kinduberre.multistrategytradingbot.model.Position;
import com.kinduberre.multistrategytradingbot.model.Signal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {
    List<Position> findByIsOpen(boolean isOpen);
    List<Position> findByStrategyAndIsOpen(Signal.Strategy strategy, boolean isOpen);
}
