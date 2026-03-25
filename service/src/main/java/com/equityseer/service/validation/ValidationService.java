package com.equityseer.service.validation;

import com.equityseer.type.TimeFrame;
import java.time.LocalDate;

public interface ValidationService {

  /**
   * Validates a stock symbol based on three specific rules:
   *
   * <p>RULE 1: Volume trend validation - if vol < prevVol < prePrevVol return false RULE 2: Average
   * volume trend validation - if avgVol < prevAvgVol < prePrevAvgVol return false RULE 3:
   * Resistance line validation - if in prev 6 months high wick touching a resistance line return
   * false
   *
   * @param symbol Stock symbol to validate
   * @param timeframe TimeFrame for the validation
   * @param date Reference date for validation
   * @return true if the stock passes all validation rules, false otherwise
   */
  boolean isValid(String symbol, TimeFrame timeframe, LocalDate date);
}
