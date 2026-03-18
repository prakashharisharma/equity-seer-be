package com.equityseer.repository.stock;

import com.equityseer.entity.stock.Stock;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {
  Optional<Stock> findBySymbol(String symbol);
}
