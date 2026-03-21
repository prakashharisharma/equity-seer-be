package com.equityseer.service.technical;

import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.modal.TechnicalIndicator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TechnicalAnalysisServiceImpl implements TechnicalAnalysisService {

  private static final double TWO = 2.0;
  private static final double HUNDRED = 100.0;

  /** Calculate current EMA value */
  private double calculateCurrentEMA(double currentClose, double previousEMA, double multiplier) {
    return (currentClose - previousEMA) * multiplier + previousEMA;
  }

  @Override
  public List<TechnicalIndicator<Double>> calculatePriceSMA(
      String symbol, List<StockOHLCV> data, int period) {
    log.debug("Calculating Price SMA for symbol: {}, period: {}", symbol, period);

    if (data == null || data.size() < period) {
      return new ArrayList<>();
    }

    List<TechnicalIndicator<Double>> result = new ArrayList<>();

    // Data is in descending order (newest first), so we need to reverse it for SMA calculation
    List<StockOHLCV> reversedData = new ArrayList<>(data);
    Collections.reverse(reversedData);

    for (int i = period - 1; i < reversedData.size(); i++) {
      double sum = 0.0;

      // Calculate sum of closing prices for period
      for (int j = 0; j < period; j++) {
        sum += reversedData.get(i - j).getClose().doubleValue();
      }

      double sma = sum / period;

      // Create TechnicalIndicator with SMA value
      TechnicalIndicator<Double> indicator = TechnicalIndicator.fromStockOHLCV(reversedData.get(i));
      indicator.withValue(sma);
      result.add(indicator);
    }

    // Reverse result back to original order (newest first)
    Collections.reverse(result);

    log.debug("Calculated Price SMA for {} data points", result.size());
    return result;
  }

  @Override
  public List<TechnicalIndicator<Double>> calculateEMA(
      String symbol, List<StockOHLCV> data, int period) {
    log.debug("Calculating EMA for symbol: {}, period: {}", symbol, period);

    if (data == null || data.size() < period) {
      return new ArrayList<>();
    }

    List<TechnicalIndicator<Double>> result = new ArrayList<>();

    // Data is in descending order (newest first), so we need to reverse it for EMA calculation
    List<StockOHLCV> reversedData = new ArrayList<>(data);
    Collections.reverse(reversedData);

    // Calculate initial SMA for first EMA value
    double initialSum = 0.0;
    for (int i = 0; i < period; i++) {
      initialSum += reversedData.get(i).getClose().doubleValue();
    }
    double previousEMA = initialSum / period;

    // Add first EMA value
    TechnicalIndicator<Double> firstEMA =
        TechnicalIndicator.fromStockOHLCV(reversedData.get(period - 1));
    firstEMA.withValue(previousEMA);
    result.add(firstEMA);

    // Calculate subsequent EMA values
    double multiplier = TWO / (period + 1);

    for (int i = period; i < reversedData.size(); i++) {
      double currentClose = reversedData.get(i).getClose().doubleValue();
      double currentEMA = calculateCurrentEMA(currentClose, previousEMA, multiplier);

      TechnicalIndicator<Double> indicator = TechnicalIndicator.fromStockOHLCV(reversedData.get(i));
      indicator.withValue(currentEMA);
      result.add(indicator);

      previousEMA = currentEMA;
    }

    // Reverse result back to original order (newest first)
    Collections.reverse(result);

    log.debug("Calculated EMA for {} data points", result.size());
    return result;
  }

  @Override
  public List<TechnicalIndicator<Double>> calculateRSI(
      String symbol, List<StockOHLCV> data, int period) {
    log.debug("Calculating RSI for symbol: {}, period: {}", symbol, period);

    if (data == null || data.size() < period + 1) {
      return new ArrayList<>();
    }

    List<TechnicalIndicator<Double>> result = new ArrayList<>();
    List<Double> gains = new ArrayList<>();
    List<Double> losses = new ArrayList<>();

    // Data is in descending order (newest first), so we need to reverse it for RSI calculation
    List<StockOHLCV> reversedData = new ArrayList<>(data);
    Collections.reverse(reversedData);

    // Calculate price changes and separate gains/losses
    for (int i = 1; i < reversedData.size(); i++) {
      double priceChange =
          reversedData.get(i).getClose().doubleValue()
              - reversedData.get(i - 1).getClose().doubleValue();

      if (priceChange > 0) {
        gains.add(priceChange);
        losses.add(0.0);
      } else {
        gains.add(0.0);
        losses.add(Math.abs(priceChange));
      }
    }

    // Calculate average gains and losses
    double avgGain = gains.stream().limit(period).reduce(0.0, Double::sum) / period;
    double avgLoss = losses.stream().limit(period).reduce(0.0, Double::sum) / period;

    // Calculate RSI for each data point
    for (int i = period; i < gains.size(); i++) {
      // Update average gains and losses
      avgGain = (avgGain * (period - 1) + gains.get(i)) / period;
      avgLoss = (avgLoss * (period - 1) + losses.get(i)) / period;

      // Calculate RSI
      double rsi;
      if (avgLoss == 0) {
        rsi = HUNDRED;
      } else {
        double rs = avgGain / avgLoss;
        rsi = HUNDRED - (HUNDRED / (1 + rs));
      }

      TechnicalIndicator<Double> indicator =
          TechnicalIndicator.fromStockOHLCV(reversedData.get(i + 1));
      indicator.withValue(rsi);
      result.add(indicator);
    }

    // Reverse result back to original order (newest first)
    Collections.reverse(result);

    log.debug("Calculated RSI for {} data points", result.size());
    return result;
  }

  @Override
  public List<TechnicalIndicator<Long>> calculateVolumeSMA(
      String symbol, List<StockOHLCV> data, int period) {
    log.debug("Calculating Volume SMA for symbol: {}, period: {}", symbol, period);

    if (data == null || data.size() < period) {
      return new ArrayList<>();
    }

    List<TechnicalIndicator<Long>> result = new ArrayList<>();

    // Data is in descending order (newest first), so we need to reverse it for Volume SMA
    // calculation
    List<StockOHLCV> reversedData = new ArrayList<>(data);
    Collections.reverse(reversedData);

    for (int i = period - 1; i < reversedData.size(); i++) {
      long volumeSum = 0L;

      // Calculate sum of volumes for period
      for (int j = 0; j < period; j++) {
        volumeSum += reversedData.get(i - j).getVolume();
      }

      long volumeSMA = volumeSum / period;

      // Create TechnicalIndicator with Volume SMA value
      TechnicalIndicator<Long> indicator = TechnicalIndicator.fromStockOHLCV(reversedData.get(i));
      indicator.withValue(volumeSMA);
      result.add(indicator);
    }

    // Reverse result back to original order (newest first)
    Collections.reverse(result);

    log.debug("Calculated Volume SMA for {} data points", result.size());
    return result;
  }
}
