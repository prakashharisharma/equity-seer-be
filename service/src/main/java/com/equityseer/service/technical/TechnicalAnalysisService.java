package com.equityseer.service.technical;

import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.modal.TechnicalIndicator;
import java.util.List;

public interface TechnicalAnalysisService {

  /**
   * Calculate Simple Moving Average (SMA) - Price
   *
   * @param symbol Stock symbol
   * @param data List of OHLCV data
   * @param period Period for SMA calculation
   * @return List of TechnicalIndicator<Double> with SMA values
   */
  List<TechnicalIndicator<Double>> calculatePriceSMA(
      String symbol, List<StockOHLCV> data, int period);

  /**
   * Calculate Exponential Moving Average (EMA)
   *
   * @param symbol Stock symbol
   * @param data List of OHLCV data
   * @param period Period for EMA calculation
   * @return List of TechnicalIndicator<Double> with EMA values
   */
  List<TechnicalIndicator<Double>> calculateEMA(String symbol, List<StockOHLCV> data, int period);

  /**
   * Calculate Relative Strength Index (RSI)
   *
   * @param symbol Stock symbol
   * @param data List of OHLCV data
   * @param period Period for RSI calculation (typically 14)
   * @return List of TechnicalIndicator<Double> with RSI values
   */
  List<TechnicalIndicator<Double>> calculateRSI(String symbol, List<StockOHLCV> data, int period);

  /**
   * Calculate Volume Simple Moving Average (Volume SMA)
   *
   * @param symbol Stock symbol
   * @param data List of OHLCV data
   * @param period Period for Volume SMA calculation
   * @return List of TechnicalIndicator<Long> with Volume SMA values
   */
  List<TechnicalIndicator<Long>> calculateVolumeSMA(
      String symbol, List<StockOHLCV> data, int period);
}
