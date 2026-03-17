package com.equityseer.stock.repository;

import com.equityseer.stock.entity.StockOHLCV;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockOHLCVRepository extends JpaRepository<StockOHLCV, Long> {
  Optional<StockOHLCV> findByNseSymbolAndDate(String nseSymbol, LocalDate date);

  List<StockOHLCV> findByNseSymbolAndDateBetween(String nseSymbol, LocalDate start, LocalDate end);

  StockOHLCV findFirstByNseSymbolOrderByDateDesc(String nseSymbol);

  default StockOHLCV findByNseSymbolOrderByDateDesc(String nseSymbol) {
    return findFirstByNseSymbolOrderByDateDesc(nseSymbol);
  }
}
