package com.equityseer.service.stock;

import com.equityseer.entity.stock.StockOHLCV;
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
}
