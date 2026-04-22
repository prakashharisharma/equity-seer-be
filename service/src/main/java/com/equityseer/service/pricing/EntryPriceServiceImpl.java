package com.equityseer.service.pricing;

import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.service.stock.StockOHLCVService;
import com.equityseer.type.TimeFrame;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntryPriceServiceImpl implements EntryPriceService {

  private final StockOHLCVService stockOHLCVService;

  @Override
  public double calculate(String symbol, TimeFrame timeframe, LocalDate date, double score) {
    List<StockOHLCV> data = stockOHLCVService.get(symbol, timeframe, 1, date);
    if (data == null || data.isEmpty()) {
      log.warn("No data found for symbol: {} at date: {}", symbol, date);
      return 0.0;
    }

    StockOHLCV cur = data.get(0);
    double close = cur.getClose().doubleValue();
    double high = cur.getHigh().doubleValue();
    double low = cur.getLow().doubleValue();
    double mid = (high + low) / 2.0;

    double entryPrice;
    if (score >= 10.0) {
      entryPrice = Math.min(close * 1.01, high);
    } else if (score >= 9.0) {
      entryPrice = close * 1.005;
    } else if (score >= 8.0) {
      entryPrice = Math.max(close * 0.99, mid);
    } else if (score >= 7.0) {
      entryPrice = mid;
    } else {
      entryPrice = low;
    }

    return Math.round(entryPrice * 100.0) / 100.0;
  }
}
