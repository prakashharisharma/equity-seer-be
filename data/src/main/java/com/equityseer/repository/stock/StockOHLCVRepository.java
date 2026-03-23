package com.equityseer.repository.stock;

import com.equityseer.entity.stock.StockOHLCV;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface StockOHLCVRepository extends JpaRepository<StockOHLCV, Long> {
  Optional<StockOHLCV> findBySymbolAndDate(String symbol, LocalDate date);

  List<StockOHLCV> findBySymbolAndDateBetween(String symbol, LocalDate start, LocalDate end);

  StockOHLCV findFirstBySymbolOrderByDateDesc(String symbol);

  default StockOHLCV findBySymbolOrderByDateDesc(String symbol) {
    return findFirstBySymbolOrderByDateDesc(symbol);
  }

  List<StockOHLCV> findAllBySymbolOrderByDateDesc(String symbol);

  List<StockOHLCV> findBySymbolAndDateLessThanEqualOrderByDateDesc(String symbol, LocalDate date);

  @Modifying
  @Transactional
  @Query("DELETE FROM StockOHLCV s WHERE s.symbol = :symbol")
  int deleteBySymbol(@Param("symbol") String symbol);
}
