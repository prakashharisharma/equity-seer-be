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
}
