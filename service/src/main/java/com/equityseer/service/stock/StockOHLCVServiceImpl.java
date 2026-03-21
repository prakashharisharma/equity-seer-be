package com.equityseer.service.stock;

import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.repository.stock.StockOHLCVRepository;
import com.equityseer.type.TimeFrame;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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

  @Override
  public List<StockOHLCV> get(String symbol, TimeFrame timeframe, int countBack) {
    requireNonBlank(symbol, "symbol must not be blank");

    if (countBack <= 0) {
      countBack = 700; // Default count
    }

    log.debug("Getting {} OHLCV data for symbol: {}, countBack: {}", timeframe, symbol, countBack);

    switch (timeframe) {
      case DAILY:
        return repository.findAllBySymbolOrderByDateDesc(symbol).stream().limit(countBack).toList();
      case WEEKLY:
        return getWeeklyData(symbol, countBack);
      case MONTHLY:
        return getMonthlyData(symbol, countBack);
      case YEARLY:
        return getYearlyData(symbol, countBack);
      default:
        throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  private List<StockOHLCV> getWeeklyData(String symbol, int countBack) {
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusWeeks(countBack);

    List<StockOHLCV> dailyData = repository.findBySymbolAndDateBetween(symbol, startDate, endDate);

    return aggregateWeeklyData(dailyData);
  }

  private List<StockOHLCV> getMonthlyData(String symbol, int countBack) {
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusMonths(countBack);

    List<StockOHLCV> dailyData = repository.findBySymbolAndDateBetween(symbol, startDate, endDate);

    return aggregateMonthlyData(dailyData);
  }

  private List<StockOHLCV> getYearlyData(String symbol, int countBack) {
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusYears(countBack);

    List<StockOHLCV> dailyData = repository.findBySymbolAndDateBetween(symbol, startDate, endDate);

    return aggregateYearlyData(dailyData);
  }

  private List<StockOHLCV> aggregateWeeklyData(List<StockOHLCV> dailyData) {
    return dailyData.stream()
        .collect(
            java.util.stream.Collectors.groupingBy(
                ohlcv -> {
                  LocalDate date = ohlcv.getDate();
                  return date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
                }))
        .entrySet()
        .stream()
        .sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey()))
        .limit(700)
        .map(
            entry -> {
              List<StockOHLCV> weekData = entry.getValue();
              return createAggregatedOHLCV(weekData, entry.getKey());
            })
        .toList();
  }

  private List<StockOHLCV> aggregateMonthlyData(List<StockOHLCV> dailyData) {
    return dailyData.stream()
        .collect(
            java.util.stream.Collectors.groupingBy(
                ohlcv ->
                    LocalDate.of(ohlcv.getDate().getYear(), ohlcv.getDate().getMonthValue(), 1)))
        .entrySet()
        .stream()
        .sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey()))
        .limit(700)
        .map(
            entry -> {
              List<StockOHLCV> monthData = entry.getValue();
              return createAggregatedOHLCV(monthData, entry.getKey());
            })
        .toList();
  }

  private List<StockOHLCV> aggregateYearlyData(List<StockOHLCV> dailyData) {
    return dailyData.stream()
        .collect(
            java.util.stream.Collectors.groupingBy(
                ohlcv -> LocalDate.of(ohlcv.getDate().getYear(), 1, 1)))
        .entrySet()
        .stream()
        .sorted((e1, e2) -> e2.getKey().compareTo(e1.getKey()))
        .limit(700)
        .map(
            entry -> {
              List<StockOHLCV> yearData = entry.getValue();
              return createAggregatedOHLCV(yearData, entry.getKey());
            })
        .toList();
  }

  private StockOHLCV createAggregatedOHLCV(List<StockOHLCV> data, LocalDate periodDate) {
    if (data.isEmpty()) {
      return null;
    }

    BigDecimal open = data.get(0).getOpen();
    BigDecimal close = data.get(data.size() - 1).getClose();

    BigDecimal high =
        data.stream().map(StockOHLCV::getHigh).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

    BigDecimal low =
        data.stream().map(StockOHLCV::getLow).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

    Long volume = data.stream().mapToLong(StockOHLCV::getVolume).sum();

    StockOHLCV aggregated = new StockOHLCV();
    aggregated.setSymbol(data.get(0).getSymbol());
    aggregated.setDate(periodDate);
    aggregated.setOpen(open);
    aggregated.setHigh(high);
    aggregated.setLow(low);
    aggregated.setClose(close);
    aggregated.setVolume(volume);

    return aggregated;
  }
}
