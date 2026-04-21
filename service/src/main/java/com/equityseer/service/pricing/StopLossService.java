package com.equityseer.service.pricing;

import com.equityseer.type.TimeFrame;
import java.time.LocalDate;

public interface StopLossService {
  /**
   * Calculates the stop loss for a given symbol, timeframe, and date.
   *
   * @param symbol the stock symbol
   * @param timeframe the timeframe
   * @param date the reference date
   * @return the calculated stop loss (low - 1%)
   */
  double calculate(String symbol, TimeFrame timeframe, LocalDate date);
}
