package com.equityseer.service.scoring;

import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.modal.TechnicalIndicator;
import com.equityseer.service.stock.StockOHLCVService;
import com.equityseer.service.technical.TechnicalAnalysisService;
import com.equityseer.type.TimeFrame;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringServiceImpl implements ScoringService {

  private static final int OHLCV_FETCH_COUNT = 700;

  private final StockOHLCVService stockOHLCVService;
  private final TechnicalAnalysisService technicalAnalysisService;

  @Override
  public double score(String symbol, TimeFrame timeframe, LocalDate date) {
    try {
      List<StockOHLCV> data = stockOHLCVService.get(symbol, timeframe, OHLCV_FETCH_COUNT, date);

      if (data == null || data.size() < 200) {
        log.debug("Insufficient data for scoring symbol: {} at {}", symbol, date);
        return 0.0;
      }

      ScoreSummary s = buildScoreSummary(symbol, data);

      double volScore = calculateVolumeScore(s);
      double maScore = calculateMAScore(s);

      double finalScore = volScore + maScore;

      finalScore -= calculatePenalties(s);
      finalScore -= addAdditionalPenalties(s, timeframe, data);

      double result = Math.max(0.0, Math.min(10.0, finalScore));
      return Math.round(result * 100.0) / 100.0;

    } catch (Exception e) {
      log.error("Error calculating score for symbol: {} at {}", symbol, date, e);
      return 0.0;
    }
  }

  // =========================
  // 🔹 VOLUME SCORE (0-5)
  // =========================
  private double calculateVolumeScore(ScoreSummary s) {

    // Strong bullish volume expansion
    if (s.v0 > s.v1 && s.v1 > s.v2) {
      return 5.0;
    }

    // Average volume trend improving
    if (s.avgV0 > s.avgV1 && s.avgV1 > s.avgV2) {
      return 4.0;
    }

    return 0.0;
  }

  // =========================
  // 🔹 MA SCORE (0-5)
  // =========================
  private double calculateMAScore(ScoreSummary s) {

    int increasingCount = 0;

    boolean ema5Inc = isIncreasing(s.ema5, s.prevEma5);
    boolean ema20Inc = isIncreasing(s.ema20, s.prevEma20);
    boolean ema50Inc = isIncreasing(s.ema50, s.prevEma50);
    boolean sma100Inc = isIncreasing(s.sma100, s.prevSma100);
    boolean sma200Inc = isIncreasing(s.sma200, s.prevSma200);

    if (ema5Inc) {
      increasingCount++;
    }
    if (ema20Inc) {
      increasingCount++;
    }
    if (ema50Inc) {
      increasingCount++;
    }
    if (sma100Inc) {
      increasingCount++;
    }
    if (sma200Inc) {
      increasingCount++;
    }

    // Rule 1: EMA5, EMA20, EMA50 all increasing
    if (ema5Inc && ema20Inc && ema50Inc) {
      return 5.0;
    }

    // Rule 2: EMA5 or EMA20 increasing + total >= 3
    if ((ema5Inc || (ema20Inc && ema50Inc)) && increasingCount >= 3) {
      return 4.0;
    }

    // Rule 3: At least 3 MAs increasing and close > EMA50
    if (increasingCount >= 3 && s.cur.getClose().doubleValue() > s.ema50) {
      return 3.0;
    }
    return 0.0;
  }

  // =========================
  // 🔹 PENALTIES
  // =========================
  private double calculatePenalties(ScoreSummary s) {
    double totalPenalty = 0.0;

    // 🔻 Volume Average Penalties
    if (s.avgV0 < s.avgV1 && s.avgV1 < s.avgV2) {
      totalPenalty += 5.0;
    } else if (s.avgV0 < s.avgV1) {
      totalPenalty += 1.0;
    }

    // 🔻 Volume Spike Penalty
    if (s.v0 > s.avgV0 * 3) {
      totalPenalty += 2.0;
    }

    return totalPenalty;
  }

  private double addAdditionalPenalties(
      ScoreSummary s, TimeFrame timeframe, List<StockOHLCV> data) {
    double v0 = s.v0;
    double v1 = s.v1;
    double avgV0 = s.avgV0;
    double close = s.cur.getClose().doubleValue();
    double penalty = 0.0;

    boolean aboveAllMAs =
        close > s.ema5
            && close > s.ema20
            && close > s.ema50
            && close > s.sma100
            && close > s.sma200;

    double monthlyGain = calculateMonthlyGain(timeframe, data);

    // Rule 1: Volume Spike Confluence (-5)
    boolean volumeCondA = (v0 > v1 * 2) && (v0 > avgV0 * 2.25);
    boolean volumeCondB = (v0 > v1 * 2.25) && (v0 > avgV0 * 2);
    if ((volumeCondA || volumeCondB) && (aboveAllMAs || monthlyGain > 0.50)) {
      penalty += 5.0;
    }

    // Rule 2: High Volume Exhaustion (-4)
    if ((v0 > avgV0 * 3.0) && (aboveAllMAs || monthlyGain > 0.40)) {
      penalty += 4.0;
    }

    // Rule 3: Extreme Volume Spike with Gain 30%
    if (v0 > avgV0 * 5.0 && monthlyGain > 0.30) {
      penalty += 3.0;
    }

    return penalty;
  }

  private double calculateMonthlyGain(TimeFrame timeframe, List<StockOHLCV> data) {
    int lookback =
        switch (timeframe) {
          case DAILY -> 20;
          case WEEKLY -> 4;
          case MONTHLY -> 1;
          default -> 0;
        };

    if (lookback > 0 && data.size() > lookback) {
      double curClose = data.get(0).getClose().doubleValue();
      double prevClose = data.get(lookback).getClose().doubleValue();
      if (prevClose > 0) {
        return (curClose - prevClose) / prevClose;
      }
    }
    return 0.0;
  }

  // =========================
  // 🔹 HELPERS
  // =========================

  private boolean isIncreasing(double cur, double prev) {
    if (prev == 0) {
      return false;
    }
    return cur > prev;
  }

  private boolean isLongUpperWick(StockOHLCV ohlcv) {
    double high = ohlcv.getHigh().doubleValue();
    double low = ohlcv.getLow().doubleValue();
    double close = ohlcv.getClose().doubleValue();
    double open = ohlcv.getOpen().doubleValue();

    double range = high - low;
    if (range <= 0) {
      return false;
    }

    double upperWick = high - Math.max(open, close);
    return (upperWick / range) > 0.4;
  }

  private ScoreSummary buildScoreSummary(String symbol, List<StockOHLCV> data) {

    List<TechnicalIndicator<Double>> ema5 = technicalAnalysisService.calculateEMA(symbol, data, 5);
    List<TechnicalIndicator<Double>> ema20 =
        technicalAnalysisService.calculateEMA(symbol, data, 20);
    List<TechnicalIndicator<Double>> ema50 =
        technicalAnalysisService.calculateEMA(symbol, data, 50);
    List<TechnicalIndicator<Double>> sma100 =
        technicalAnalysisService.calculatePriceSMA(symbol, data, 100);
    List<TechnicalIndicator<Double>> sma200 =
        technicalAnalysisService.calculatePriceSMA(symbol, data, 200);
    List<TechnicalIndicator<Long>> volSma20 =
        technicalAnalysisService.calculateVolumeSMA(symbol, data, 20);

    return ScoreSummary.builder()
        .cur(data.get(0))
        .prev(data.get(1))

        // Volume
        .v0(data.get(0).getVolume().doubleValue())
        .v1(data.get(1).getVolume().doubleValue())
        .v2(data.get(2).getVolume().doubleValue())

        // Avg Volume blocks (using Volume SMA with period 5)
        .avgV0(volSma20.get(0).getValue().doubleValue())
        .avgV1(volSma20.get(5).getValue().doubleValue())
        .avgV2(volSma20.get(10).getValue().doubleValue())

        // MAs
        .ema5(ema5.get(0).getValue())
        .prevEma5(ema5.get(1).getValue())
        .ema20(ema20.get(0).getValue())
        .prevEma20(ema20.get(1).getValue())
        .ema50(ema50.get(0).getValue())
        .prevEma50(ema50.get(1).getValue())
        .sma100(sma100.get(0).getValue())
        .prevSma100(sma100.get(1).getValue())
        .sma200(sma200.get(0).getValue())
        .prevSma200(sma200.get(1).getValue())
        .build();
  }

  @Builder
  private static class ScoreSummary {

    StockOHLCV cur;
    StockOHLCV prev;

    double v0, v1, v2;
    double avgV0, avgV1, avgV2;
    double ema5, prevEma5;
    double ema20, prevEma20;
    double ema50, prevEma50;
    double sma100, prevSma100;
    double sma200, prevSma200;
  }
}
