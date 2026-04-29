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
public class TargetServiceImpl implements TargetService {

  private final StockOHLCVService stockOHLCVService;

  @Override
  public double calculate(String symbol, TimeFrame timeframe, LocalDate date, double entry) {
    List<StockOHLCV> data = stockOHLCVService.get(symbol, timeframe, 36, date);
    if (data == null || data.isEmpty()) {
      log.warn("No data found for symbol: {} at date: {}", symbol, date);
      return entry * 1.50; // Fallback to entry + 50%
    }

    double currentClose = data.get(0).getClose().doubleValue();

    List<Double> highs =
        data.stream()
            .map(ohlcv -> ohlcv.getHigh().doubleValue())
            .sorted((a, b) -> Double.compare(b, a))
            .toList();

    double maxHigh = highs.isEmpty() ? 0.0 : highs.get(0);
    double secondHigh = highs.size() > 1 ? highs.get(1) : maxHigh;

    double target;
    if (maxHigh > currentClose) {
      target = Math.max(maxHigh * 0.90, secondHigh * 0.95);
    } else {
      target = entry * 1.50; // target is entry + 50%
    }

    return Math.round(target * 100.0) / 100.0;
  }
}
