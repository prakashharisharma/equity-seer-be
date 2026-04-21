package com.equityseer.service.pricing;

import com.equityseer.type.TimeFrame;
import java.time.LocalDate;

public interface EntryPriceService {
  /**
   * Calculates the entry price for a given symbol, timeframe, and date.
   *
   * @param symbol the stock symbol
   * @param timeframe the timeframe
   * @param date the reference date
   * @return the calculated entry price (close + 1%)
   */
  double calculate(String symbol, TimeFrame timeframe, LocalDate date);
}
