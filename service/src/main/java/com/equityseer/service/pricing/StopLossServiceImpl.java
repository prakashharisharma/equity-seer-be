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
public class StopLossServiceImpl implements StopLossService {

  private final StockOHLCVService stockOHLCVService;

  @Override
  public double calculate(String symbol, TimeFrame timeframe, LocalDate date) {
    List<StockOHLCV> data = stockOHLCVService.get(symbol, timeframe, 3, date);
    if (data == null || data.isEmpty()) {
      log.warn("No data found for symbol: {} at date: {}", symbol, date);
      return 0.0;
    }

    double minLow =
        data.stream().map(ohlcv -> ohlcv.getLow().doubleValue()).min(Double::compare).orElse(0.0);

    double stopLoss = minLow * 0.99; // lowest low - 1%
    return Math.round(stopLoss * 100.0) / 100.0;
  }
}
