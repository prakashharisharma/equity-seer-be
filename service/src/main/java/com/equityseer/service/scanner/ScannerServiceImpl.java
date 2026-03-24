package com.equityseer.service.scanner;

import com.equityseer.entity.stock.Stock;
import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.modal.TechnicalIndicator;
import com.equityseer.service.StockService;
import com.equityseer.service.stock.StockOHLCVService;
import com.equityseer.service.technical.TechnicalAnalysisService;
import com.equityseer.type.TimeFrame;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScannerServiceImpl implements ScannerService {

  private static final int OHLCV_FETCH_COUNT = 700;

  private final StockService stockService;
  private final StockOHLCVService stockOHLCVService;
  private final TechnicalAnalysisService technicalAnalysisService;

  @Override
  public List<Stock> scanEmaAlignmentWithMomentum(TimeFrame timeframe, LocalDate date) {
    log.info(
        "Starting EMA Alignment with Momentum scan for timeframe: {}, date: {}...",
        timeframe,
        date);

    List<Stock> allStocks = stockService.list();
    List<Stock> matchingStocks = new ArrayList<>();

    for (Stock stock : allStocks) {
      try {
        String symbol = stock.getSymbol();
        List<StockOHLCV> data = stockOHLCVService.get(symbol, timeframe, OHLCV_FETCH_COUNT, date);

        // Need at least 200 data points for SMA 200
        if (data == null || data.size() < 200) {
          log.debug(
              "Skipping symbol: {} due to insufficient data (count: {})",
              symbol,
              data != null ? data.size() : 0);
          continue;
        }

        List<TechnicalIndicator<Double>> ema5 =
            technicalAnalysisService.calculateEMA(symbol, data, 5);
        List<TechnicalIndicator<Double>> ema20 =
            technicalAnalysisService.calculateEMA(symbol, data, 20);
        List<TechnicalIndicator<Double>> ema50 =
            technicalAnalysisService.calculateEMA(symbol, data, 50);
        List<TechnicalIndicator<Double>> sma200 =
            technicalAnalysisService.calculatePriceSMA(symbol, data, 200);

        // Need at least 3 points for current, previous, and prev-prev session analysis
        if (ema5.size() < 3 || ema20.size() < 3 || ema50.isEmpty() || sma200.isEmpty()) {
          continue;
        }

        // Current session values (index 0)
        double curEma5 = ema5.get(0).getValue();
        double curEma20 = ema20.get(0).getValue();
        double curEma50 = ema50.get(0).getValue();
        double curSma200 = sma200.get(0).getValue();

        // Previous session values (index 1)
        double prevEma5 = ema5.get(1).getValue();
        double prevEma20 = ema20.get(1).getValue();

        // Prev-Prev session values (index 2)
        double prevPrevEma5 = ema5.get(2).getValue();
        double prevPrevEma20 = ema20.get(2).getValue();

        // 1. Check if EMA5 and EMA20 are increasing (current vs previous)
        boolean isIncreasing = curEma5 >= prevEma5 && curEma20 >= prevEma20;

        // 2. Check if EMA5 and EMA20 were decreasing in previous session (previous vs prev-prev)
        boolean wasDecreasing = prevEma5 < prevPrevEma5 && prevEma20 < prevPrevEma20;

        // 3. Check if EMA alignment is bullish
        boolean isBullishAligned =
            curEma5 >= curEma20 && curEma20 >= curEma50 && curEma50 >= curSma200;

        if (isIncreasing && wasDecreasing && isBullishAligned) {
          matchingStocks.add(stock);
          log.debug("Stock matched scan criteria: {}", symbol);
        }

      } catch (Exception e) {
        log.error("Error scanning stock: {}", stock.getSymbol(), e);
      }
    }

    log.info(
        "Scan completed. Found {} stocks matching the criteria: {}",
        matchingStocks.size(),
        matchingStocks.stream().map(Stock::getSymbol).toList());

    return matchingStocks;
  }

  @Override
  public List<Stock> scanVolumeExpansionWithPriceActionSignal(TimeFrame timeframe, LocalDate date) {
    log.info(
        "Starting Volume Expansion with Price Action Signal scan for timeframe: {}, date: {}...",
        timeframe,
        date);

    List<Stock> allStocks = stockService.list();
    List<Stock> matchingStocks = new ArrayList<>();

    for (Stock stock : allStocks) {
      try {
        String symbol = stock.getSymbol();
        List<StockOHLCV> data = stockOHLCVService.get(symbol, timeframe, OHLCV_FETCH_COUNT, date);

        if (data == null || data.size() < 200) {
          continue;
        }

        StockOHLCV cur = data.get(0);
        StockOHLCV prev = data.get(1);

        // 4. Candle must be green
        if (cur.getClose().compareTo(cur.getOpen()) <= 0) {
          continue;
        }

        // 1. Liquidity Filter: 3-session Avg Volume ≥ 1,000,000
        double avgVol0 = calculateAvgVolume(data, 0, 3);
        if (avgVol0 < 1_000_000) {
          continue;
        }

        if (!hasVolumeExpansion(data, avgVol0)) {
          continue;
        }

        if (hasPriceActionSignal(symbol, data, cur, prev)) {
          matchingStocks.add(stock);
          log.debug("Stock matched scan criteria: {}", symbol);
        }

      } catch (Exception e) {
        log.error("Error scanning stock for volume expansion: {}", stock.getSymbol(), e);
      }
    }

    log.info(
        "Volume Expansion scan completed. Found {} stocks matching the criteria: {}",
        matchingStocks.size(),
        matchingStocks.stream().map(Stock::getSymbol).toList());

    return matchingStocks;
  }

  private boolean hasVolumeExpansion(List<StockOHLCV> data, double avgVol0) {
    StockOHLCV cur = data.get(0);
    StockOHLCV prev = data.get(1);

    double v0 = cur.getVolume().doubleValue();
    double v1 = prev.getVolume().doubleValue();
    double v2 = data.get(2).getVolume().doubleValue();
    double v3 = data.get(3).getVolume().doubleValue();

    double avgVol1 = calculateAvgVolume(data, 1, 3);
    double avgVol2 = calculateAvgVolume(data, 2, 3);

    boolean conditionA = v0 > v1 && v1 > v2;
    boolean conditionB = avgVol0 > avgVol1 && avgVol1 > avgVol2;
    boolean conditionC = (avgVol0 > avgVol1) && (v1 > v2 && v2 > v3);

    return conditionA || conditionB || conditionC;
  }

  private boolean hasPriceActionSignal(
      String symbol, List<StockOHLCV> data, StockOHLCV cur, StockOHLCV prev) {
    List<TechnicalIndicator<Double>> ema5 = technicalAnalysisService.calculateEMA(symbol, data, 5);
    List<TechnicalIndicator<Double>> ema20 =
        technicalAnalysisService.calculateEMA(symbol, data, 20);
    List<TechnicalIndicator<Double>> ema50 =
        technicalAnalysisService.calculateEMA(symbol, data, 50);
    List<TechnicalIndicator<Double>> sma100 =
        technicalAnalysisService.calculatePriceSMA(symbol, data, 100);
    List<TechnicalIndicator<Double>> sma200 =
        technicalAnalysisService.calculatePriceSMA(symbol, data, 200);

    double curClose = cur.getClose().doubleValue();
    double prevClose = prev.getClose().doubleValue();
    double curLow = cur.getLow().doubleValue();
    double curHigh = cur.getHigh().doubleValue();

    double ema5Val = ema5.get(0).getValue();
    double ema20Val = ema20.get(0).getValue();
    double ema50Val = ema50.get(0).getValue();
    double sma100Val = sma100.get(0).getValue();
    double sma200Val = sma200.get(0).getValue();

    boolean ema5Signal = isSignalAtLevel(ema5Val, curClose, prevClose, curLow, curHigh);
    boolean ema20Signal = isSignalAtLevel(ema20Val, curClose, prevClose, curLow, curHigh);
    boolean ema50Signal = isSignalAtLevel(ema50Val, curClose, prevClose, curLow, curHigh);
    boolean sma100Signal = isSignalAtLevel(sma100Val, curClose, prevClose, curLow, curHigh);
    boolean sma200Signal = isSignalAtLevel(sma200Val, curClose, prevClose, curLow, curHigh);

    boolean ema5IsHighest =
        ema5Val > ema20Val && ema5Val > ema50Val && ema5Val > sma100Val && ema5Val > sma200Val;
    boolean onlyEma5Signal =
        ema5Signal && !ema20Signal && !ema50Signal && !sma100Signal && !sma200Signal;

    if (onlyEma5Signal && ema5IsHighest) {
      log.debug("Ignoring signal for {}: EMA5 breakout/rejection while EMA5 is highest.", symbol);
      return false;
    }

    return ema5Signal || ema20Signal || ema50Signal || sma100Signal || sma200Signal;
  }

  private boolean isSignalAtLevel(
      double level, double curClose, double prevClose, double curLow, double curHigh) {
    boolean breakout = prevClose < level && curClose > level;
    boolean rejection =
        curLow < level && curClose > level && isCloseInTopRange(curClose, curLow, curHigh, 0.3);
    return breakout || rejection;
  }

  private double calculateAvgVolume(List<StockOHLCV> data, int startIndex, int period) {
    if (data.size() < startIndex + period) {
      return 0;
    }
    double sum = 0;
    for (int i = 0; i < period; i++) {
      sum += data.get(startIndex + i).getVolume().doubleValue();
    }
    return sum / period;
  }

  private boolean isCloseInTopRange(double close, double low, double high, double topRangePercent) {
    double range = high - low;
    if (range <= 0) {
      return false;
    }
    double threshold = low + (1 - topRangePercent) * range;
    return close >= threshold;
  }
}
