package com.equityseer.service;

import com.equityseer.entity.stock.Stock;
import java.util.List;
import java.util.Optional;

public interface StockService {
  Stock create(String symbol, String name, String isin, String series);

  List<Stock> list();

  Optional<Stock> findBySymbol(String symbol);

  Optional<Stock> findById(Long id);

  Stock update(Long id, String symbol, String name, String isin, String series);

  void delete(Long id);
}
