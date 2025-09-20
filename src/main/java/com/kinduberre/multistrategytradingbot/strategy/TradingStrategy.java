package com.kinduberre.multistrategytradingbot.strategy;

import com.kinduberre.multistrategytradingbot.model.MarketData;
import com.kinduberre.multistrategytradingbot.model.Signal;

import java.util.List;

public interface TradingStrategy {
    Signal analyze(List<MarketData> marketData);
}
