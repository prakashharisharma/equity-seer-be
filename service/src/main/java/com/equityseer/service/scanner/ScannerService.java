package com.equityseer.service.scanner;

import com.equityseer.entity.stock.Stock;
import com.equityseer.type.TimeFrame;
import java.time.LocalDate;
import java.util.List;

public interface ScannerService {

  /**
   * Scan for stocks that exhibit EMA alignment with momentum.
   *
   * @param timeframe Timeframe for the scan
   * @param date Date to scan for (typically current date)
   * @return List of stocks matching the criteria
   */
  List<Stock> scanEmaAlignmentWithMomentum(TimeFrame timeframe, LocalDate date);

  /**
   * Scan for stocks that exhibit volume expansion with price action signals.
   *
   * @param timeframe Timeframe for the scan
   * @param date Date to scan for
   * @return List of stocks matching the criteria
   */
  List<Stock> scanVolumeExpansionWithPriceActionSignal(TimeFrame timeframe, LocalDate date);
}
