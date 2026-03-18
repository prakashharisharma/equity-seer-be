package com.equityseer.entity.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "stock_ohlcvs",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_stock_ohlcv_symbol_date",
          columnNames = {"symbol", "date"})
    },
    indexes = {
      @Index(name = "idx_stock_ohlcv_symbol", columnList = "symbol"),
      @Index(name = "idx_stock_ohlcv_symbol_date", columnList = "symbol,date")
    })
public class StockOHLCV {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private LocalDate date;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal open;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal high;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal low;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal close;

  @Column(nullable = false)
  private Long volume;

  @Column(name = "symbol", nullable = false, length = 32)
  private String symbol;
}
