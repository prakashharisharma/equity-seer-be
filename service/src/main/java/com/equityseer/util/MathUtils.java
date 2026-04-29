package com.equityseer.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MathUtils {

  /**
   * Calculates the percentage change from a base value to a target value.
   *
   * @param base the starting value
   * @param target the ending value
   * @return the percentage change (e.g., 50.0 for a 50% increase)
   */
  public static double calculatePercentageChange(double base, double target) {
    if (base == 0) {
      return 0.0;
    }
    double change = ((target - base) / base) * 100.0;
    return Math.round(change * 100.0) / 100.0;
  }
}
