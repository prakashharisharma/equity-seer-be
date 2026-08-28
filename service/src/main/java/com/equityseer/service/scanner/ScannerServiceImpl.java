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
  private static final int REJECTION_LOOKBACK_CANDLES = 60;
  private static final int REQUIRED_REJECTION_COUNT = 3;

  private static final double LOWER_WICK_RATIO = 1.5;
  private static final double TOLERANCE_FACTOR = 0.25;
  private static final double MIN_TOLERANCE = 0.5;
  private static final int MIN_TOUCH_SEPARATION = 2;

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

    // allStocks.clear();
    // allStocks.add(stockService.findBySymbol("DCBBANK").get());

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

        // 4. Candle must be green OR have lower wick > upper wick
        // OR in downtrend (lower low lower high) with upper wick > lower wick
        double open = cur.getOpen().doubleValue();
        double close = cur.getClose().doubleValue();
        double high = cur.getHigh().doubleValue();
        double low = cur.getLow().doubleValue();

        boolean isGreen = close > open;
        double upperWick = high - Math.max(open, close);
        double lowerWick = Math.min(open, close) - low;
        boolean lowerWickGreater = lowerWick > upperWick * 2;
        boolean upperWickGreater = upperWick > lowerWick * 2;
        boolean hasLowerLowLowerHighPattern = this.hasLowerLowLowerHighPattern(data);

        if (!(isGreen || lowerWickGreater || (hasLowerLowLowerHighPattern && upperWickGreater))) {
          continue;
        }

        // 1. Liquidity Filter: 10-session Avg Volume ≥ 1,000,000
        List<TechnicalIndicator<Long>> volSma10 =
            technicalAnalysisService.calculateVolumeSMA(symbol, data, 10);

        if (volSma10 == null || volSma10.isEmpty()) {
          continue;
        }

        double avgVol0 = volSma10.getFirst().getValue().doubleValue();

        if (avgVol0 < 10_00_000) {
          continue;
        }

        if (!hasVolumeExpansion(data, volSma10)) {
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

  private boolean hasVolumeExpansion(
      List<StockOHLCV> data, List<TechnicalIndicator<Long>> volSma10) {
    StockOHLCV cur = data.get(0);
    StockOHLCV prev = data.get(1);

    double v0 = cur.getVolume().doubleValue();
    double v1 = prev.getVolume().doubleValue();
    double v2 = data.get(2).getVolume().doubleValue();

    // Use volSma20 values. Note: volSma20 has same order as data (descending)
    double avgVol0 = volSma10.get(0).getValue().doubleValue();
    double avgVol1 = volSma10.get(1).getValue().doubleValue();
    double avgVol2 = volSma10.get(2).getValue().doubleValue();

    boolean conditionA = v0 > v1 && v1 > v2;
    boolean conditionB = v0 < v1 && v1 < v2;
    boolean conditionC = avgVol0 > avgVol1 && avgVol1 > avgVol2;

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

    double ema5Val = ema5.getFirst().getValue();
    double ema20Val = ema20.getFirst().getValue();
    double ema50Val = ema50.getFirst().getValue();
    double sma100Val = sma100.getFirst().getValue();
    double sma200Val = sma200.getFirst().getValue();

    boolean ema5Signal = isSignalAtLevel(ema5, curClose, prevClose, curLow, curHigh);
    boolean ema20Signal = isSignalAtLevel(ema20, curClose, prevClose, curLow, curHigh);
    boolean ema50Signal = isSignalAtLevel(ema50, curClose, prevClose, curLow, curHigh);
    boolean sma100Signal = isSignalAtLevel(sma100, curClose, prevClose, curLow, curHigh);
    boolean sma200Signal = isSignalAtLevel(sma200, curClose, prevClose, curLow, curHigh);

    boolean ema5IsHighest =
        ema5Val > ema20Val && ema5Val > ema50Val && ema5Val > sma100Val && ema5Val > sma200Val;
    boolean onlyEma5Signal =
        ema5Signal && !ema20Signal && !ema50Signal && !sma100Signal && !sma200Signal;

    if (onlyEma5Signal && ema5IsHighest) {
      log.debug("Ignoring signal for {}: EMA5 breakout/rejection while EMA5 is highest.", symbol);
      return false;
    }

    boolean hasHorizontalSupport = this.hasHorizontalSupport(data, 60);

    return hasHorizontalSupport
        || ema5Signal
        || ema20Signal
        || ema50Signal
        || sma100Signal
        || sma200Signal;
  }

  private boolean isSignalAtLevel(
      List<TechnicalIndicator<Double>> indicators,
      double curClose,
      double prevClose,
      double curLow,
      double curHigh) {
    if (indicators == null || indicators.isEmpty()) {
      return false;
    }

    double level = indicators.getFirst().getValue();
    return isBreakoutAtLevel(level, curClose, prevClose)
        || hasRejectionAtLevel(indicators, level, curClose, curLow, curHigh);
  }

  private boolean isBreakoutAtLevel(double level, double curClose, double prevClose) {
    return prevClose <= level && curClose > level;
  }

  private boolean hasRejectionAtLevel(
      List<TechnicalIndicator<Double>> indicators,
      double level,
      double curClose,
      double curLow,
      double curHigh) {
    if (!isRejectionAtLevel(level, curClose, curLow, curHigh)) {
      return false;
    }

    int rejectionCount = 0;
    int lookback = Math.min(REJECTION_LOOKBACK_CANDLES, indicators.size());

    for (int i = 0; i < lookback; i++) {
      TechnicalIndicator<Double> indicator = indicators.get(i);
      TechnicalIndicator<Double> next = i > 0 ? indicators.get(i - 1) : null;
      if (isRejectionAtLevel(indicator, next)) {
        rejectionCount++;
        if (rejectionCount >= REQUIRED_REJECTION_COUNT) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean isRejectionAtLevel(double level, double curClose, double curLow, double curHigh) {
    return curLow <= level
        && curClose > level; // && isCloseInTopRange(curClose, curLow, curHigh, 0.3);
  }

  private boolean isRejectionAtLevel(
      TechnicalIndicator<Double> indicator, TechnicalIndicator<Double> next) {
    return indicator.getLow() <= indicator.getValue()
        && indicator.getClose() > indicator.getValue()
        && isConfirmedByNextCandle(indicator, next); // && isCloseInTopRange(..., 0.3);
  }

  private boolean isConfirmedByNextCandle(
      TechnicalIndicator<Double> indicator, TechnicalIndicator<Double> next) {
    if (next == null) {
      return false;
    }

    boolean nextIsGreen = next.getClose() > next.getOpen();
    boolean nextHasHigherHighHigherLow =
        next.getHigh() > indicator.getHigh() && next.getLow() > indicator.getLow();

    return nextIsGreen || nextHasHigherHighHigherLow;
  }

  private boolean isCloseInTopRange(double close, double low, double high, double topRangePercent) {
    double range = high - low;
    if (range <= 0) {
      return false;
    }
    double threshold = low + (1 - topRangePercent) * range;
    return close >= threshold;
  }

  /**
   * Detect horizontal support (markedLow) across recent candles with tolerance.
   *
   * <p>Rules: - For each possible markedLow chosen as the low of a candle within the last {@code
   * scanBack} candles (including current at index 0), consider the interval from current (index 0)
   * to that marked index m. - In that interval (indices 0..m inclusive), we require at least {@code
   * requiredMatches} candles that "touch" the markedLow: touch = min(open, close) > markedLow &&
   * low <= markedLow + tolerance where tolerance = markedLow * 0.005 (0.5% of markedLow) - There
   * must NOT be any intermediary candle (between current and marked index) with close < markedLow.
   * - Only consider a markedLow if either: a) the markedLow candle itself is red (close < open), OR
   * b) the candle immediately closer to present than markedLow (index m-1) is red.
   *
   * <p>Assumes data in descending chronological order: index 0 = current, index increases into the
   * past.
   */
  private boolean hasHorizontalSupport(List<StockOHLCV> data, int scanBack) {

    if (data == null || data.size() < 2) {
      return false;
    }

    int limit = Math.min(scanBack, data.size() - 1);

    for (int m = 0; m <= limit; m++) {

      double supportLevel = data.get(m).getLow().doubleValue();

      if (!isCurrentTouchingSupport(data.getFirst(), supportLevel)) {
        continue;
      }

      List<Integer> touches = findTouches(data, supportLevel, limit);

      if (isValidSupport(touches)) {
        return true;
      }
    }

    return false;
  }

  private boolean isCurrentTouchingSupport(StockOHLCV current, double supportLevel) {

    double low = current.getLow().doubleValue();

    double bodyBottom = Math.min(current.getOpen().doubleValue(), current.getClose().doubleValue());

    return low <= supportLevel && bodyBottom > supportLevel;
  }

  private List<Integer> findTouches(List<StockOHLCV> data, double supportLevel, int limit) {

    List<Integer> touches = new ArrayList<>();

    for (int i = 0; i <= limit; i++) {

      StockOHLCV candle = data.get(i);

      if (isTouch(candle, supportLevel)) {
        touches.add(i);
      }
    }

    return touches;
  }

  private boolean isTouch(StockOHLCV candle, double supportLevel) {

    double low = candle.getLow().doubleValue();

    double bodyBottom = Math.min(candle.getOpen().doubleValue(), candle.getClose().doubleValue());

    return low <= supportLevel && bodyBottom > supportLevel;
  }

  private boolean isValidSupport(List<Integer> touches) {

    if (touches.size() < 2) {
      return false;
    }

    if (hasNonAdjacentTouches(touches)) {
      return true;
    }

    return hasThreeConsecutiveTouches(touches);
  }

  private boolean hasNonAdjacentTouches(List<Integer> touches) {

    for (int i = 1; i < touches.size(); i++) {

      if (touches.get(i) - touches.get(i - 1) > 1) {
        return true;
      }
    }

    return false;
  }

  private boolean hasThreeConsecutiveTouches(List<Integer> touches) {

    int consecutive = 1;

    for (int i = 1; i < touches.size(); i++) {

      if (touches.get(i) - touches.get(i - 1) == 1) {

        consecutive++;

        if (consecutive >= 3) {
          return true;
        }

      } else {

        consecutive = 1;
      }
    }

    return false;
  }

  /**
   * Detect if the last 6 candles show a downtrend pattern (lower lows and lower highs).
   *
   * <p>Checks if at least 3 out of 5 consecutive candle pairs (in the last 6 candles, going from
   * past to present) show lower low AND lower high (downtrend).
   *
   * <p>Data is in descending order: index 0 = current, index increases into past.
   */
  private boolean hasLowerLowLowerHighPattern(List<StockOHLCV> data) {
    if (data == null || data.size() < 6) {
      return false;
    }

    int downtrendCount = 0;

    // Check comparisons from index 5 to 0 (going from past to present)
    // Compare each candle with the one closer to present
    for (int i = 5; i > 0; i--) {
      double olderLow = data.get(i).getLow().doubleValue();
      double olderHigh = data.get(i).getHigh().doubleValue();
      double newerLow = data.get(i - 1).getLow().doubleValue();
      double newerHigh = data.get(i - 1).getHigh().doubleValue();

      // Downtrend: newer candle has lower low AND lower high than older candle
      if (newerLow < olderLow && newerHigh < olderHigh) {
        downtrendCount++;
      }
    }

    // Require at least 3 out of 5 comparisons to show downtrend
    return downtrendCount >= 3;
  }
}
