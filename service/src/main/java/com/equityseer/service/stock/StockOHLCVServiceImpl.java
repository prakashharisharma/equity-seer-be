package com.equityseer.service.stock;

import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.repository.stock.StockOHLCVRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class StockOHLCVServiceImpl implements StockOHLCVService {
  private final StockOHLCVRepository repository;

  public StockOHLCVServiceImpl(StockOHLCVRepository repository) {
    this.repository = repository;
  }

  @Override
  public StockOHLCV save(StockOHLCV ohlcv) {
    validate(ohlcv);
    log.debug("Saving OHLCV symbol={} date={}", ohlcv.getSymbol(), ohlcv.getDate());
    return repository.save(ohlcv);
  }

  @Override
  @Transactional
  public List<StockOHLCV> saveAll(List<StockOHLCV> ohlcvs) {
    Objects.requireNonNull(ohlcvs, "ohlcvs must not be null");
    for (StockOHLCV ohlcv : ohlcvs) {
      validate(ohlcv);
    }
    log.info("Saving {} OHLCV rows", ohlcvs.size());
    return repository.saveAll(ohlcvs);
  }

  @Override
  public List<StockOHLCV> findBySymbolAndDateRange(String symbol, LocalDate start, LocalDate end) {
    requireNonBlank(symbol, "symbol must not be blank");
    Objects.requireNonNull(start, "start must not be null");
    Objects.requireNonNull(end, "end must not be null");
    if (end.isBefore(start)) {
      throw new IllegalArgumentException("end must be >= start");
    }
    return repository.findBySymbolAndDateBetween(symbol, start, end);
  }

  @Override
  public Optional<StockOHLCV> findBySymbolAndDate(String symbol, LocalDate date) {
    requireNonBlank(symbol, "symbol must not be blank");
    Objects.requireNonNull(date, "date must not be null");
    return repository.findBySymbolAndDate(symbol, date);
  }

  @Override
  public Optional<StockOHLCV> findLatestBySymbol(String symbol) {
    requireNonBlank(symbol, "symbol must not be blank");
    return Optional.ofNullable(repository.findFirstBySymbolOrderByDateDesc(symbol));
  }

  @Override
  @Transactional
  public StockOHLCV upsert(StockOHLCV ohlcv) {
    validate(ohlcv);
    String symbol = ohlcv.getSymbol();
    LocalDate date = ohlcv.getDate();

    Optional<StockOHLCV> existingOpt = repository.findBySymbolAndDate(symbol, date);
    if (existingOpt.isPresent()) {
      StockOHLCV existing = existingOpt.get();
      existing.setOpen(ohlcv.getOpen());
      existing.setHigh(ohlcv.getHigh());
      existing.setLow(ohlcv.getLow());
      existing.setClose(ohlcv.getClose());
      existing.setVolume(ohlcv.getVolume());
      log.info("Updated OHLCV symbol={} date={}", symbol, date);
      return repository.save(existing);
    }

    log.info("Inserted OHLCV symbol={} date={}", symbol, date);
    return repository.save(ohlcv);
  }

  private static void validate(StockOHLCV ohlcv) {
    Objects.requireNonNull(ohlcv, "ohlcv must not be null");

    Objects.requireNonNull(ohlcv.getDate(), "date must not be null");
    requireNonBlank(ohlcv.getSymbol(), "symbol must not be blank");

    requireNonNullDecimal(ohlcv.getOpen(), "open must not be null");
    BigDecimal high = requireNonNullDecimal(ohlcv.getHigh(), "high must not be null");
    BigDecimal low = requireNonNullDecimal(ohlcv.getLow(), "low must not be null");
    requireNonNullDecimal(ohlcv.getClose(), "close must not be null");

    Long volume = Objects.requireNonNull(ohlcv.getVolume(), "volume must not be null");
    if (volume <= 0) {
      throw new IllegalArgumentException("volume must be positive");
    }

    if (high.compareTo(low) < 0) {
      throw new IllegalArgumentException("high must be >= low");
    }
  }

  private static BigDecimal requireNonNullDecimal(BigDecimal value, String message) {
    return Objects.requireNonNull(value, message);
  }

  private static void requireNonBlank(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
  }
}
