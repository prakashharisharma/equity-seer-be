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

  // Weights
  private static final double VOL_WEIGHT = 0.40;
  private static final double MA_WEIGHT = 0.60;

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

      double finalScore = (volScore * VOL_WEIGHT) + (maScore * MA_WEIGHT);

      // 🔻 Long upper wick penalty
      if (isLongUpperWick(s.cur)) {
        finalScore -= 0.05;
      }

      double result = Math.max(0.0, finalScore * 10);
      return Math.min(10.0, result);

    } catch (Exception e) {
      log.error("Error calculating score for symbol: {} at {}", symbol, date, e);
      return 0.0;
    }
  }

  // =========================
  // 🔹 VOLUME SCORE
  // =========================
  private double calculateVolumeScore(ScoreSummary s) {

    double ratio = s.v0 / (s.avgV1 == 0 ? 1 : s.avgV1);

    if (ratio > 2.0) {
      return 1.0;
    }
    if (ratio > 1.5) {
      return 0.8;
    }
    if (ratio > 1.2) {
      return 0.6;
    }

    if (s.v0 > s.v1 && s.v1 > s.v2) {
      return 0.5;
    }

    return 0.0;
  }

  // =========================
  // 🔹 MA TREND SCORE
  // =========================
  private double calculateMAScore(ScoreSummary s) {

    double score = 0;

    if (isIncreasing(s.ema5, s.prevEma5, 0.001)) {
      score += 0.5;
    }
    if (isIncreasing(s.ema20, s.prevEma20, 0.001)) {
      score += 1.0;
    }
    if (isIncreasing(s.ema50, s.prevEma50, 0.001)) {
      score += 1.5;
    }
    if (isIncreasing(s.sma100, s.prevSma100, 0.001)) {
      score += 2.0;
    }
    if (isIncreasing(s.sma200, s.prevSma200, 0.001)) {
      score += 3.0;
    }

    score = score / 8.0;

    if (s.ema20 > s.ema50 && s.ema50 > s.sma100 && s.sma100 > s.sma200) {
      score = Math.min(1.0, score + 0.1);
    }

    return score;
  }

  // =========================
  // 🔹 HELPERS
  // =========================

  private boolean isIncreasing(double cur, double prev, double threshold) {
    return (cur - prev) / prev > threshold;
  }

  private boolean isCloseInTopRange(double close, double low, double high, double pct) {
    double range = high - low;
    if (range <= 0) {
      return false;
    }
    return (high - close) / range <= pct;
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

    StockOHLCV cur = data.get(0);
    StockOHLCV prev = data.get(1);

    return ScoreSummary.builder()
        .cur(cur)
        .prev(prev)
        .v0(cur.getVolume().doubleValue())
        .v1(prev.getVolume().doubleValue())
        .v2(data.get(2).getVolume().doubleValue())
        .v3(data.get(3).getVolume().doubleValue())
        .avgV1(calculateAvgVolume(data, 1, 5))
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

  private double calculateAvgVolume(List<StockOHLCV> data, int start, int period) {
    double sum = 0;
    for (int i = 0; i < period; i++) {
      sum += data.get(start + i).getVolume().doubleValue();
    }
    return sum / period;
  }

  @Builder
  private static class ScoreSummary {
    StockOHLCV cur;
    StockOHLCV prev;
    double v0, v1, v2, v3;
    double avgV1;
    double ema5, prevEma5;
    double ema20, prevEma20;
    double ema50, prevEma50;
    double sma100, prevSma100;
    double sma200, prevSma200;
  }
}
