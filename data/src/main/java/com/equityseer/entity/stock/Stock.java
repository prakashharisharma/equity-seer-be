package com.equityseer.entity.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "stocks",
    uniqueConstraints = {@UniqueConstraint(columnNames = "symbol", name = "uk_stocks_symbol")},
    indexes = {@Index(columnList = "symbol", name = "idx_stocks_symbol")})
public class Stock {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 20)
  private String symbol;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false, length = 12)
  private String isin;

  @Column(nullable = false, length = 50)
  private String series;

  @Column(nullable = false)
  private Boolean activityCompleted = false;

  protected Stock() {}

  public Stock(String symbol, String name, String isin, String series) {
    this.symbol = symbol;
    this.name = name;
    this.isin = isin;
    this.series = series;
    this.activityCompleted = false;
  }

  public Long getId() {
    return id;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getIsin() {
    return isin;
  }

  public void setIsin(String isin) {
    this.isin = isin;
  }

  public String getSeries() {
    return series;
  }

  public void setSeries(String series) {
    this.series = series;
  }

  public Boolean getActivityCompleted() {
    return activityCompleted;
  }

  public void setActivityCompleted(Boolean activityCompleted) {
    this.activityCompleted = activityCompleted;
  }
}
