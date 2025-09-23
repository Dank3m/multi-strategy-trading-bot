package com.kinduberre.multistrategytradingbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinduberre.multistrategytradingbot.model.MarketData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BinanceWebSocketService {

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentLinkedQueue<MarketData> dataQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService reconnectScheduler = Executors.newScheduledThreadPool(1);

    private WebSocket webSocket;
    private MarketDataListener marketDataListener;

    @Value("${websocket.binance.stream-url}")
    private String streamUrl;

    @Value("${websocket.binance.reconnect-interval}")
    private long reconnectInterval;

    @PostConstruct
    public void initialize() {
        connect();
    }

    public void connect() {
        String url = streamUrl + "/btcusdt@kline_1m";

        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("WebSocket connected to Binance");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                processMessage(text);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                log.warn("WebSocket closing: {} - {}", code, reason);
                webSocket.close(1000, null);
                scheduleReconnect();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("WebSocket failure", t);
                scheduleReconnect();
            }
        });
    }

    private void processMessage(String message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, Map.class);

            if (data.containsKey("k")) {
                Map<String, Object> kline = (Map<String, Object>) data.get("k");

                MarketData marketData = new MarketData();
                marketData.setSymbol((String) kline.get("s"));
                marketData.setOpen(new BigDecimal(kline.get("o").toString()));
                marketData.setHigh(new BigDecimal(kline.get("h").toString()));
                marketData.setLow(new BigDecimal(kline.get("l").toString()));
                marketData.setClose(new BigDecimal(kline.get("c").toString()));
                marketData.setVolume(new BigDecimal(kline.get("v").toString()));
                marketData.setTimestamp(LocalDateTime.now());

                dataQueue.offer(marketData);

                // Keep queue size manageable
                while (dataQueue.size() > 1000) {
                    dataQueue.poll();
                }

                // Notify listeners
                if (marketDataListener != null) {
                    marketDataListener.onMarketData(marketData);
                }
            }
        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
        }
    }

    private void scheduleReconnect() {
        reconnectScheduler.schedule(() -> {
            log.info("Attempting to reconnect WebSocket");
            connect();
        }, reconnectInterval, TimeUnit.MILLISECONDS);
    }

    public void subscribeToMultipleStreams(List<String> symbols) {
        StringBuilder streams = new StringBuilder();
        for (String symbol : symbols) {
            streams.append(symbol.toLowerCase()).append("@kline_1m/");
            streams.append(symbol.toLowerCase()).append("@trade/");
        }

        String url = streamUrl + "/stream?streams=" + streams.toString();
        // Connect with multiple streams
    }

    public ConcurrentLinkedQueue<MarketData> getDataQueue() {
        return dataQueue;
    }

    public void setMarketDataListener(MarketDataListener listener) {
        this.marketDataListener = listener;
    }

    public interface MarketDataListener {
        void onMarketData(MarketData data);
    }
}