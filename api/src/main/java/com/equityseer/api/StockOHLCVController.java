package com.equityseer.api;

import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.service.stock.StockOHLCVService;
import com.equityseer.type.TimeFrame;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/ohlcv")
@RequiredArgsConstructor
public class StockOHLCVController {

  private final StockOHLCVService stockOHLCVService;

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<StockOHLCV>> getOHLCV(
      @RequestParam("symbol") String symbol,
      @RequestParam(value = "timeframe", defaultValue = "DAILY") TimeFrame timeframe,
      @RequestParam(value = "count", defaultValue = "700") int count) {

    log.info(
        "Received OHLCV request for symbol: {}, timeframe: {}, count: {}",
        symbol,
        timeframe,
        count);

    try {
      List<StockOHLCV> ohlcvData = stockOHLCVService.get(symbol, timeframe, count);
      return ResponseEntity.ok(ohlcvData);
    } catch (Exception e) {
      log.error("Error fetching OHLCV data for symbol: {}", symbol, e);
      return ResponseEntity.internalServerError().build();
    }
  }
}
