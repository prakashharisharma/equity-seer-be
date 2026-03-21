package com.equityseer.modal;

import com.equityseer.entity.stock.StockOHLCV;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class TechnicalIndicator<T> {

  private String symbol;
  private String date;
  private Double open;
  private Double high;
  private Double low;
  private Double close;
  private Long volume;

  // Generic technical indicator value
  private T value;

  /** Create TechnicalIndicator from StockOHLCV */
  public static <T> TechnicalIndicator<T> fromStockOHLCV(StockOHLCV ohlcv) {
    TechnicalIndicator<T> indicator = new TechnicalIndicator<>();
    indicator.setSymbol(ohlcv.getSymbol());
    indicator.setDate(ohlcv.getDate().toString());
    indicator.setOpen(ohlcv.getOpen().doubleValue());
    indicator.setHigh(ohlcv.getHigh().doubleValue());
    indicator.setLow(ohlcv.getLow().doubleValue());
    indicator.setClose(ohlcv.getClose().doubleValue());
    indicator.setVolume(ohlcv.getVolume());
    return indicator;
  }

  /** Set technical indicator value (with chaining) */
  public TechnicalIndicator<T> withValue(T value) {
    this.value = value;
    return this;
  }
}
