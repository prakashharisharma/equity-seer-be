package com.equityseer.service.stock;

import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.type.TimeFrame;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockOHLCVService {
  StockOHLCV save(StockOHLCV ohlcv);

  List<StockOHLCV> saveAll(List<StockOHLCV> ohlcvs);

  List<StockOHLCV> findBySymbolAndDateRange(String symbol, LocalDate start, LocalDate end);

  Optional<StockOHLCV> findBySymbolAndDate(String symbol, LocalDate date);

  Optional<StockOHLCV> findLatestBySymbol(String symbol);

  StockOHLCV upsert(StockOHLCV ohlcv);

  List<StockOHLCV> get(String symbol, TimeFrame timeframe, int countBack);
}
