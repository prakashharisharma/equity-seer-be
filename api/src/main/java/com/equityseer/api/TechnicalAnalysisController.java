package com.equityseer.api;

import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.modal.TechnicalIndicator;
import com.equityseer.service.stock.StockOHLCVService;
import com.equityseer.service.technical.TechnicalAnalysisService;
import com.equityseer.type.TimeFrame;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/technical")
@RequiredArgsConstructor
public class TechnicalAnalysisController {

  private static final int OHLCV_FETCH_COUNT = 700;

  private final TechnicalAnalysisService technicalAnalysisService;
  private final StockOHLCVService stockOHLCVService;

  @GetMapping(value = "/sma/price", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<TechnicalIndicator<Double>>> calculatePriceSMA(
      @RequestParam("symbol") String symbol,
      @RequestParam(value = "timeframe", defaultValue = "DAILY") TimeFrame timeframe,
      @RequestParam(value = "period", defaultValue = "20") int period,
      @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {

    log.info(
        "Calculating Price SMA for symbol: {}, timeframe: {}, period: {}, date: {}",
        symbol,
        timeframe,
        period,
        date);

    try {
      if (period <= 0) {
        return ResponseEntity.badRequest().build();
      }

      LocalDate effectiveDate = date != null ? date : LocalDate.now();
      List<StockOHLCV> data =
          stockOHLCVService.get(symbol, timeframe, OHLCV_FETCH_COUNT, effectiveDate);

      List<TechnicalIndicator<Double>> smaData =
          technicalAnalysisService.calculatePriceSMA(symbol, data, period);

      return ResponseEntity.ok(smaData);
    } catch (Exception e) {
      log.error("Error calculating Price SMA for symbol: {}", symbol, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(value = "/sma/volume", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<TechnicalIndicator<Long>>> calculateVolumeSMA(
      @RequestParam("symbol") String symbol,
      @RequestParam(value = "timeframe", defaultValue = "DAILY") TimeFrame timeframe,
      @RequestParam(value = "period", defaultValue = "20") int period,
      @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {

    log.info(
        "Calculating Volume SMA for symbol: {}, timeframe: {}, period: {}, date: {}",
        symbol,
        timeframe,
        period,
        date);

    try {
      if (period <= 0) {
        return ResponseEntity.badRequest().build();
      }

      LocalDate effectiveDate = date != null ? date : LocalDate.now();
      List<StockOHLCV> data =
          stockOHLCVService.get(symbol, timeframe, OHLCV_FETCH_COUNT, effectiveDate);

      List<TechnicalIndicator<Long>> volumeSmaData =
          technicalAnalysisService.calculateVolumeSMA(symbol, data, period);

      return ResponseEntity.ok(volumeSmaData);
    } catch (Exception e) {
      log.error("Error calculating Volume SMA for symbol: {}", symbol, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(value = "/ema/price", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<TechnicalIndicator<Double>>> calculateEMA(
      @RequestParam("symbol") String symbol,
      @RequestParam(value = "timeframe", defaultValue = "DAILY") TimeFrame timeframe,
      @RequestParam(value = "period", defaultValue = "20") int period,
      @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {

    log.info(
        "Calculating EMA for symbol: {}, timeframe: {}, period: {}, date: {}",
        symbol,
        timeframe,
        period,
        date);

    try {
      if (period <= 0) {
        return ResponseEntity.badRequest().build();
      }

      LocalDate effectiveDate = date != null ? date : LocalDate.now();
      List<StockOHLCV> data =
          stockOHLCVService.get(symbol, timeframe, OHLCV_FETCH_COUNT, effectiveDate);

      List<TechnicalIndicator<Double>> emaData =
          technicalAnalysisService.calculateEMA(symbol, data, period);

      return ResponseEntity.ok(emaData);
    } catch (Exception e) {
      log.error("Error calculating EMA for symbol: {}", symbol, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(value = "/rsi", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<TechnicalIndicator<Double>>> calculateRSI(
      @RequestParam("symbol") String symbol,
      @RequestParam(value = "timeframe", defaultValue = "DAILY") TimeFrame timeframe,
      @RequestParam(value = "period", defaultValue = "14") int period,
      @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate date) {

    log.info(
        "Calculating RSI for symbol: {}, timeframe: {}, period: {}, date: {}",
        symbol,
        timeframe,
        period,
        date);

    try {
      if (period <= 0) {
        return ResponseEntity.badRequest().build();
      }

      LocalDate effectiveDate = date != null ? date : LocalDate.now();
      List<StockOHLCV> data =
          stockOHLCVService.get(symbol, timeframe, OHLCV_FETCH_COUNT, effectiveDate);

      List<TechnicalIndicator<Double>> rsiData =
          technicalAnalysisService.calculateRSI(symbol, data, period);

      return ResponseEntity.ok(rsiData);
    } catch (Exception e) {
      log.error("Error calculating RSI for symbol: {}", symbol, e);
      return ResponseEntity.internalServerError().build();
    }
  }
}
