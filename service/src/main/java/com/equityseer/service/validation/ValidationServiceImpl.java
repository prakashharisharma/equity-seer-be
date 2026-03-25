package com.equityseer.service.validation;

import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.modal.TechnicalIndicator;
import com.equityseer.service.stock.StockOHLCVService;
import com.equityseer.service.technical.TechnicalAnalysisService;
import com.equityseer.type.TimeFrame;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationServiceImpl implements ValidationService {

  private static final int OHLCV_FETCH_COUNT = 200;
  private static final int VOLUME_SMA_PERIOD = 20;
  private static final int MONTHS_TO_CHECK = 6;

  private final StockOHLCVService stockOHLCVService;
  private final TechnicalAnalysisService technicalAnalysisService;

  @Override
  public boolean isValid(String symbol, TimeFrame timeframe, LocalDate date) {
    try {
      List<StockOHLCV> data = stockOHLCVService.get(symbol, timeframe, OHLCV_FETCH_COUNT, date);

      if (data == null || data.size() < 3) {
        log.debug("Insufficient data for validation symbol: {} at {}", symbol, date);
        return false;
      }

      // Rule 1: Volume trend validation
      if (!validateVolumeTrend(symbol, data)) {
        log.debug(
            "Rule 1 failed: Volume trend validation failed for symbol: {} at {}", symbol, date);
        return false;
      }

      // Rule 2: Average volume trend validation
      if (!validateAverageVolumeTrend(symbol, data)) {
        log.debug(
            "Rule 2 failed: Average volume trend validation failed for symbol: {} at {}",
            symbol,
            date);
        return false;
      }

      // Rule 3: Resistance line validation
      if (!validateResistanceLine(symbol, data, date)) {
        log.debug(
            "Rule 3 failed: Resistance line validation failed for symbol: {} at {}", symbol, date);
        return false;
      }

      // Rule 4: Monthly Wick validation
      if (shouldBypassWickCheck(symbol, data, date)) {
        return true;
      } else if (validateMonthlyWickStructure(symbol, data, date)) {
        log.debug("Rule 4 failed: Wick validation failed for symbol: {} at {}", symbol, date);
        return false;
      }

      return true;

    } catch (Exception e) {
      log.error("Error validating symbol: {} at {}", symbol, date, e);
      return false;
    }
  }

  /**
   * RULE 1: Volume trend validation Returns false if vol < prevVol < prePrevVol (decreasing volume
   * trend) AND average volume is not increasing
   */
  private boolean validateVolumeTrend(String symbol, List<StockOHLCV> data) {
    if (data.size() < 3) {
      return false;
    }

    double vol0 = data.get(0).getVolume().doubleValue();
    double vol1 = data.get(1).getVolume().doubleValue();
    double vol2 = data.get(2).getVolume().doubleValue();

    // If volume is strictly decreasing, check if average volume is also decreasing
    if (vol0 < vol1 && vol1 < vol2) {
      // Volume is decreasing, now check average volume trend
      return validateAverageVolumeTrendForVolumeCheck(symbol, data);
    }

    // Volume is not strictly decreasing, so this rule passes
    return true;
  }

  /** Helper method to check average volume trend specifically for volume validation rule */
  private boolean validateAverageVolumeTrendForVolumeCheck(String symbol, List<StockOHLCV> data) {
    try {
      List<TechnicalIndicator<Long>> volumeSMA =
          technicalAnalysisService.calculateVolumeSMA(symbol, data, VOLUME_SMA_PERIOD);

      if (volumeSMA == null || volumeSMA.size() < 3) {
        return true; // If we can't calculate average volume, don't fail the validation
      }

      double avgVol0 = volumeSMA.get(0).getValue().doubleValue();
      double avgVol1 = volumeSMA.get(1).getValue().doubleValue();
      double avgVol2 = volumeSMA.get(2).getValue().doubleValue();

      // Check if average volume is increasing
      boolean isAvgIncreasing = avgVol0 > avgVol1 && avgVol1 > avgVol2;

      // If avg is NOT increasing → FAIL
      if (!isAvgIncreasing) {
        return false;
      }

      return true;

    } catch (Exception e) {
      log.error("Error calculating average volume trend for volume validation", e);
      return true; // If we can't calculate, don't fail the validation
    }
  }

  /**
   * RULE 2: Average volume trend validation Returns false if avgVol < prevAvgVol < prePrevAvgVol
   * (decreasing average volume trend) AND volume is also not increasing
   */
  private boolean validateAverageVolumeTrend(String symbol, List<StockOHLCV> data) {
    try {
      List<TechnicalIndicator<Long>> volumeSMA =
          technicalAnalysisService.calculateVolumeSMA(symbol, data, VOLUME_SMA_PERIOD);

      if (volumeSMA == null || volumeSMA.size() < 3) {
        return false;
      }

      double avgVol0 = volumeSMA.get(0).getValue().doubleValue();
      double avgVol1 = volumeSMA.get(1).getValue().doubleValue();
      double avgVol2 = volumeSMA.get(2).getValue().doubleValue();

      // Check if average volume is decreasing
      if (avgVol0 < avgVol1 && avgVol1 < avgVol2) {
        return validateVolumeTrendForAverageVolumeCheck(data);
      }

      // If avg volume NOT decreasing → PASS
      return true;

    } catch (Exception e) {
      log.error("Error calculating average volume trend for symbol", e);
      return false;
    }
  }

  /** Helper method to check volume trend specifically for average volume validation rule */
  private boolean validateVolumeTrendForAverageVolumeCheck(List<StockOHLCV> data) {
    if (data.size() < 3) {
      return true; // If we can't calculate volume trend, don't fail the validation
    }

    double vol0 = data.get(0).getVolume().doubleValue();
    double vol1 = data.get(1).getVolume().doubleValue();
    double vol2 = data.get(2).getVolume().doubleValue();

    // Check if volume is increasing
    boolean isVolumeIncreasing = vol0 > vol1 && vol1 > vol2;

    // If volume is NOT increasing → FAIL
    if (!isVolumeIncreasing) {
      return false;
    }

    return true;
  }

  /**
   * RULE 3: Resistance line validation Returns false if in previous 6 months high wick touched a
   * resistance line Resistance level is not the highest high, but a level where >= 2 highs
   * intercept, but MACD(open,close) below
   */
  private boolean validateResistanceLine(
      String symbol, List<StockOHLCV> data, LocalDate referenceDate) {
    try {
      // Get data for the past 6 months
      LocalDate sixMonthsAgo = referenceDate.minus(MONTHS_TO_CHECK, ChronoUnit.MONTHS);

      // Filter data to only include dates within the last 6 months
      List<StockOHLCV> recentData =
          data.stream().filter(ohlcv -> !ohlcv.getDate().isBefore(sixMonthsAgo)).toList();

      if (recentData.size() < 20) {
        return true; // Not enough data to determine resistance
      }

      // Find resistance levels where >= 2 highs intercept
      List<Double> resistanceLevels = findResistanceLevels(recentData);

      if (resistanceLevels.isEmpty()) {
        return true; // No resistance levels found
      }

      // Check if any high wick touched any resistance level
      // We'll consider a "touch" if the high is within 0.5% of any resistance level
      for (double resistanceLevel : resistanceLevels) {
        double tolerance = resistanceLevel * 0.005;

        boolean touched =
            recentData.stream()
                .anyMatch(
                    ohlcv -> {
                      double high = ohlcv.getHigh().doubleValue();
                      return Math.abs(high - resistanceLevel) <= tolerance;
                    });

        if (touched) {
          // Check if Max(open,close) is below the resistance level
          if (isMaxBelowResistance(symbol, recentData, resistanceLevel)) {
            return false; // Failed validation: touched resistance and Max(open,close) is below
          }
        }
      }

      return true; // No resistance levels were touched, or MACD was not below

    } catch (Exception e) {
      log.error("Error validating resistance line for symbol: {}", symbol, e);
      return false;
    }
  }

  /** Find resistance levels where >= 2 highs intercept */
  private List<Double> findResistanceLevels(List<StockOHLCV> data) {
    // Group highs by rounded price levels (to account for small variations)
    Map<Double, Integer> priceLevelCounts = new HashMap<>();

    for (StockOHLCV ohlcv : data) {
      double high = ohlcv.getHigh().doubleValue();
      // Round to nearest 0.5 to group similar price levels
      double roundedLevel = Math.round(high * 2.0) / 2.0;

      priceLevelCounts.merge(roundedLevel, 1, Integer::sum);
    }

    // Find levels that were touched at least 2 times
    return priceLevelCounts.entrySet().stream()
        .filter(entry -> entry.getValue() >= 2)
        .map(Map.Entry::getKey)
        .sorted(Collections.reverseOrder()) // Sort in descending order (highest resistance first)
        .toList();
  }

  /** Check if Max(open,close) is below the resistance level */
  private boolean isMaxBelowResistance(
      String symbol, List<StockOHLCV> data, double resistanceLevel) {
    try {
      // Calculate Max(open,close) for each data point
      List<Double> maxPriceData =
          data.stream()
              .map(ohlcv -> Math.max(ohlcv.getOpen().doubleValue(), ohlcv.getClose().doubleValue()))
              .toList();

      // Check if the latest Max(open,close) is below the resistance level
      if (!maxPriceData.isEmpty()) {
        double latestMaxPrice = maxPriceData.get(maxPriceData.size() - 1);
        return latestMaxPrice < resistanceLevel;
      }

      return false;

    } catch (Exception e) {
      log.error("Error calculating Max(open,close) for resistance validation", e);
      return false; // If we can't calculate, don't fail the validation
    }
  }

  private boolean validateMonthlyWickStructure(
      String symbol, List<StockOHLCV> data, LocalDate date) {
    if (data == null || data.size() < 3) {
      return true;
    }

    int upperWickCount = 0;
    int weakLowerWickCount = 0;

    int lookback = Math.min(data.size() - 1, 6);

    for (int i = 1; i <= lookback; i++) {
      StockOHLCV c = data.get(i);

      double high = c.getHigh().doubleValue();
      double low = c.getLow().doubleValue();
      double open = c.getOpen().doubleValue();
      double close = c.getClose().doubleValue();

      double range = high - low;
      if (range == 0) {
        continue;
      }

      double upperWick = high - Math.max(open, close);
      double lowerWick = Math.min(open, close) - low;

      double upperRatio = upperWick / range;
      double lowerRatio = lowerWick / range;

      // Extreme upper wick → always bad (distribution)
      if (upperRatio > 0.6) {
        return false;
      }

      // Extreme lower wick → context based
      if (lowerRatio > 0.6) {
        if (isWeakClose(data, i)) {
          return false;
        }
      }

      // Count upper wick (always bearish)
      if (upperRatio > 0.4) {
        upperWickCount++;
      }

      // Count only WEAK lower wicks
      if (lowerRatio > 0.4 && isWeakClose(data, i)) {
        weakLowerWickCount++;
      }
    }

    // Repeated selling pressure
    if (upperWickCount >= 3) {
      return false;
    }

    // Repeated failed demand
    if (weakLowerWickCount >= 2) {
      return false;
    }

    return true;
  }

  private boolean isWeakClose(List<StockOHLCV> data, int i) {
    StockOHLCV c = data.get(i);

    double open = c.getOpen().doubleValue();
    double close = c.getClose().doubleValue();

    // 1. Red candle
    if (close < open) {
      return true;
    }

    // 2. Falling vs previous candle
    if (i + 1 < data.size()) {
      double prevClose = data.get(i + 1).getClose().doubleValue();
      if (close < prevClose) {
        return true;
      }
    }

    return false;
  }

  private boolean shouldBypassWickCheck(String symbol, List<StockOHLCV> data, LocalDate date) {

    if (data.isEmpty()) {
      return false;
    }

    List<TechnicalIndicator<Double>> ema50 =
        technicalAnalysisService.calculateEMA(symbol, data, 50);

    StockOHLCV current = data.get(0);
    double close = current.getClose().doubleValue();
    double low = current.getLow().doubleValue();
    double high = current.getHigh().doubleValue();
    double open = current.getOpen().doubleValue();

    double ema = ema50.get(0).getValue();

    // Condition 1: EMA reclaim
    boolean emaReclaim = close > ema && low <= ema;

    // Condition 2: Strong bullish candle
    double range = high - low;
    if (range == 0) {
      return emaReclaim;
    }

    double body = Math.abs(close - open);
    double upperWick = high - Math.max(open, close);
    double lowerWick = Math.min(open, close) - low;

    double bodyRatio = body / range;
    double upperRatio = upperWick / range;
    double lowerRatio = lowerWick / range;

    boolean strongBullishCandle =
        close > open
            && // green
            bodyRatio > 0.5
            && // strong body
            lowerRatio > 0.3
            && // decent lower wick
            upperRatio < 0.2; // small upper wick

    return emaReclaim || strongBullishCandle;
  }
}
