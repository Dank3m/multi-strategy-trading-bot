package com.kinduberre.multistrategytradingbot.controller;

import com.kinduberre.multistrategytradingbot.config.BacktestConfig;
import com.kinduberre.multistrategytradingbot.model.BacktestResult;
import com.kinduberre.multistrategytradingbot.model.MarketData;
import com.kinduberre.multistrategytradingbot.service.BacktestingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestingService backtestingService;

    @PostMapping("/run/file")
    public BacktestResult runBacktestFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "10000") double initialCapital,
            @RequestParam(defaultValue = "0.01") double riskPerTrade,
            @RequestParam(defaultValue = "5") int maxPositions) {

        try {
            List<MarketData> historicalData = parseFileData(file);

            BacktestConfig config = new BacktestConfig();
            config.setRiskPerTrade(BigDecimal.valueOf(riskPerTrade));
            config.setMaxPositions(maxPositions);

            return backtestingService.runBacktest(
                    historicalData,
                    BigDecimal.valueOf(initialCapital),
                    config
            );
        } catch (Exception e) {
            log.error("Error running backtest: ", e);
            throw new RuntimeException("Failed to process file: " + e.getMessage());
        }
    }

    @PostMapping("/run/filepath")
    public BacktestResult runBacktestFromFilePath(
            @RequestParam String filepath,
            @RequestParam(defaultValue = "10000") double initialCapital,
            @RequestParam(defaultValue = "0.01") double riskPerTrade,
            @RequestParam(defaultValue = "5") int maxPositions) {

        try {
            List<MarketData> historicalData = parseFileFromPath(filepath);

            BacktestConfig config = new BacktestConfig();
            config.setRiskPerTrade(BigDecimal.valueOf(riskPerTrade));
            config.setMaxPositions(maxPositions);

            return backtestingService.runBacktest(
                    historicalData,
                    BigDecimal.valueOf(initialCapital),
                    config
            );
        } catch (Exception e) {
            log.error("Error running backtest: ", e);
            throw new RuntimeException("Failed to process file: " + e.getMessage());
        }
    }

    private List<MarketData> parseFileData(MultipartFile file) throws IOException {
        List<MarketData> dataList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {

            String line = reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                MarketData data = parseLine(line);
                if (data != null) {
                    dataList.add(data);
                }
            }
        }

        return dataList;
    }

    private List<MarketData> parseFileFromPath(String filepath) throws IOException {
        List<MarketData> dataList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line = reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                MarketData data = parseLine(line);
                if (data != null) {
                    dataList.add(data);
                }
            }
        }

        return dataList;
    }

    private MarketData parseLine(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length < 10) return null;

            MarketData data = new MarketData();

            // Parse Unix timestamp to LocalDateTime
            long unixTimestamp = Long.parseLong(parts[0]);
            data.setTimestamp(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(unixTimestamp),
                    ZoneId.systemDefault()
            ));

            // Skip date field (parts[1]) as we already have timestamp
            data.setSymbol(parts[2]);
            data.setOpen(new BigDecimal(parts[3]));
            data.setHigh(new BigDecimal(parts[4]));
            data.setLow(new BigDecimal(parts[5]));
            data.setClose(new BigDecimal(parts[6]));
            data.setVolume(new BigDecimal(parts[7]));
            data.setVolumeUsdt(new BigDecimal(parts[8]));
            data.setTradeCount(Long.parseLong(parts[9]));

            return data;
        } catch (Exception e) {
            log.warn("Failed to parse line: {}", line);
            return null;
        }
    }
}