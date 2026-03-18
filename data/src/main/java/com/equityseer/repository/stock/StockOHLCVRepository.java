package com.equityseer.repository.stock;

import com.equityseer.entity.stock.StockOHLCV;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockOHLCVRepository extends JpaRepository<StockOHLCV, Long> {
  Optional<StockOHLCV> findBySymbolAndDate(String symbol, LocalDate date);

  List<StockOHLCV> findBySymbolAndDateBetween(String symbol, LocalDate start, LocalDate end);

  StockOHLCV findFirstBySymbolOrderByDateDesc(String symbol);

  default StockOHLCV findBySymbolOrderByDateDesc(String symbol) {
    return findFirstBySymbolOrderByDateDesc(symbol);
  }
}
