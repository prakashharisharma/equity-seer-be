package com.equityseer.service;

import com.equityseer.entity.stock.Stock;
import com.equityseer.repository.stock.StockRepository;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class StockServiceImpl implements StockService {
  private final StockRepository stockRepository;

  public StockServiceImpl(StockRepository stockRepository) {
    this.stockRepository = stockRepository;
  }

  @Override
  @Transactional
  public Stock create(String symbol, String name, String isin, String series) {
    log.info(
        "Creating stock with symbol={}, name={}, isin={}, series={}", symbol, name, isin, series);
    return stockRepository.save(new Stock(symbol, name, isin, series));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Stock> list() {
    return stockRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Stock> findBySymbol(String symbol) {
    log.info("Finding stock by symbol={}", symbol);
    return stockRepository.findBySymbol(symbol);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Stock> findById(Long id) {
    return stockRepository.findById(id);
  }

  @Override
  @Transactional
  public Stock update(Long id, String symbol, String name, String isin, String series) {
    log.info(
        "Updating stock with id={}, symbol={}, name={}, isin={}, series={}",
        id,
        symbol,
        name,
        isin,
        series);
    return stockRepository
        .findById(id)
        .map(
            stock -> {
              stock.setSymbol(symbol);
              stock.setName(name);
              stock.setIsin(isin);
              stock.setSeries(series);
              return stockRepository.save(stock);
            })
        .orElseThrow(() -> new IllegalArgumentException("Stock not found with id: " + id));
  }

  @Override
  @Transactional
  public void delete(Long id) {
    log.info("Deleting stock with id={}", id);
    stockRepository.deleteById(id);
  }
}
