package com.equityseer.service.bhav;

import com.equityseer.entity.stock.Stock;
import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.modal.Bhav;
import com.equityseer.repository.stock.StockOHLCVRepository;
import com.equityseer.service.StockService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring DI services and repositories are not externally mutable")
public class BhavProcessorImpl implements BhavProcessor {

  private final StockService stockService;

  private final StockOHLCVRepository stockOHLCVRepository;

  public BhavProcessorImpl(StockService stockService, StockOHLCVRepository stockOHLCVRepository) {
    this.stockService = stockService;
    this.stockOHLCVRepository = stockOHLCVRepository;
  }

  @Override
  public void process(List<Bhav> bhavList) {
    Objects.requireNonNull(bhavList, "bhavList must not be null");

    for (Bhav bhav : bhavList) {
      Optional<Stock> existingStock = stockService.findBySymbol(bhav.getSymbol());

      if (existingStock.isPresent()) {
        processExistingStock(bhav, existingStock.get());
      } else {
        processNewStock(bhav);
      }
    }
  }

  private void processExistingStock(Bhav bhav, Stock stock) {
    // Save OHLCV data
    StockOHLCV ohlcv = transform(bhav);
    stockOHLCVRepository.save(ohlcv);

    // Update series if different
    updateStockSeriesIfNeeded(bhav, stock);

    logProcessedStock(ohlcv, "existing");
  }

  private void processNewStock(Bhav bhav) {
    // Create stock first
    Stock newStock =
        stockService.create(
            bhav.getSymbol(), bhav.getCompanyName(), bhav.getIsin(), bhav.getSeries());

    // Then save OHLCV data
    StockOHLCV ohlcv = transform(bhav);
    stockOHLCVRepository.save(ohlcv);

    log.info("Created new stock id={} for symbol={}", newStock.getId(), bhav.getSymbol());
    logProcessedStock(ohlcv, "new");
  }

  private void updateStockSeriesIfNeeded(Bhav bhav, Stock stock) {
    if (!Objects.equals(stock.getSeries(), bhav.getSeries())) {
      log.info(
          "Updating series for {}: '{}' -> '{}'",
          bhav.getSymbol(),
          stock.getSeries(),
          bhav.getSeries());

      stockService.update(
          stock.getId(), stock.getSymbol(), stock.getName(), stock.getIsin(), bhav.getSeries());
    }
  }

  private void logProcessedStock(StockOHLCV ohlcv, String stockType) {
    log.info(
        "Processed {} stock - {}: {} @ {}/{}/{}/{} vol={}",
        stockType,
        ohlcv.getSymbol(),
        ohlcv.getDate(),
        ohlcv.getOpen(),
        ohlcv.getHigh(),
        ohlcv.getLow(),
        ohlcv.getClose(),
        ohlcv.getVolume());
  }

  private static StockOHLCV transform(Bhav bhav) {
    Objects.requireNonNull(bhav, "bhav must not be null");

    StockOHLCV o = new StockOHLCV();
    o.setSymbol(bhav.getSymbol());
    o.setDate(parseDateOrNull(bhav.getTimestamp(), bhav.getBizDt()));
    o.setOpen(toBigDecimal(bhav.getOpen()));
    o.setHigh(toBigDecimal(bhav.getHigh()));
    o.setLow(toBigDecimal(bhav.getLow()));
    o.setClose(toBigDecimal(bhav.getClose()));
    o.setVolume(bhav.getTottrdqty());
    return o;
  }

  private static LocalDate parseDateOrNull(String tradDt, String bizDt) {
    LocalDate parsed = parseDate(tradDt);
    return parsed != null ? parsed : parseDate(bizDt);
  }

  private static LocalDate parseDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String v = value.trim();

    // Common NSE formats seen in bhavcopy exports
    LocalDate parsed;
    parsed = tryParse(v, DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
    if (parsed != null) {
      return parsed;
    }
    parsed = tryParse(v, DateTimeFormatter.ISO_LOCAL_DATE); // yyyy-MM-dd
    if (parsed != null) {
      return parsed;
    }
    parsed = tryParse(v, DateTimeFormatter.ofPattern("dd-MMM-uuuu", Locale.ENGLISH)); // 17-Mar-2026
    if (parsed != null) {
      return parsed;
    }
    return tryParse(v, DateTimeFormatter.ofPattern("dd/MM/uuuu")); // 17/03/2026
  }

  private static LocalDate tryParse(String value, DateTimeFormatter formatter) {
    try {
      return LocalDate.parse(value, formatter);
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }

  private static BigDecimal toBigDecimal(Double value) {
    if (value == null) {
      return null;
    }
    // keep scale stable for DB columns (scale=4)
    return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
  }
}
