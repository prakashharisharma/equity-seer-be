package com.equityseer.service.bhav;

import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.modal.Bhav;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BhavProcessorImpl implements BhavProcessor {

  @Override
  public void process(List<Bhav> bhavList) {
    Objects.requireNonNull(bhavList, "bhavList must not be null");

    for (Bhav bhav : bhavList) {
      StockOHLCV ohlcv = transform(bhav);
      log.info(
          "Transformed Bhav -> StockOHLCV symbol={} date={} o={} h={} l={} c={} v={}",
          ohlcv.getNseSymbol(),
          ohlcv.getDate(),
          ohlcv.getOpen(),
          ohlcv.getHigh(),
          ohlcv.getLow(),
          ohlcv.getClose(),
          ohlcv.getVolume());
    }
  }

  private static StockOHLCV transform(Bhav bhav) {
    Objects.requireNonNull(bhav, "bhav must not be null");

    StockOHLCV o = new StockOHLCV();
    o.setNseSymbol(bhav.getNseSymbol());
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
