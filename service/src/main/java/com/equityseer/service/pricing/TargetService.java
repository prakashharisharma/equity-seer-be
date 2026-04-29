package com.equityseer.service.pricing;

import com.equityseer.type.TimeFrame;
import java.time.LocalDate;

public interface TargetService {
  /**
   * Calculates the target price for a given symbol, timeframe, date, and entry price.
   *
   * @param symbol the stock symbol
   * @param timeframe the timeframe
   * @param date the reference date
   * @param entry the entry price
   * @return the calculated target price
   */
  double calculate(String symbol, TimeFrame timeframe, LocalDate date, double entry);
}
