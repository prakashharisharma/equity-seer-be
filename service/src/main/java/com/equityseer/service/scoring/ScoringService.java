package com.equityseer.service.scoring;

import com.equityseer.type.TimeFrame;
import java.time.LocalDate;

public interface ScoringService {

  /**
   * Calculate a score for a given stock symbol at a specific date and timeframe.
   *
   * @param symbol Stock symbol
   * @param timeframe Timeframe for calculations
   * @param date The reference date
   * @return A score between 0 and 10
   */
  double score(String symbol, TimeFrame timeframe, LocalDate date);
}
