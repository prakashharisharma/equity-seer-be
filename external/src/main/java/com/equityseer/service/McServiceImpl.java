package com.equityseer.service;

import static java.time.ZoneOffset.UTC;

import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.modal.MCResult;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class McServiceImpl implements McService {

  @Override
  public List<StockOHLCV> getOHLCV(String nseSymbol, int years, int countback) {

    LocalDateTime to = LocalDateTime.of(2026, 3, 18, 00, 00, 00, 000);
    // LocalDateTime to = LocalDate.now().atStartOfDay();

    LocalDateTime from = to.minusYears(years);

    RestTemplate restTemplate = new RestTemplate();

    String baseUrl = "https://priceapi.moneycontrol.com/techCharts/indianMarket/stock/history";

    String resolution = "1D";
    String currencyCode = "INR";

    URI uri =
        UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParam("symbol", nseSymbol) // Automatically encodes special characters
            .queryParam("resolution", resolution)
            .queryParam("from", from.toEpochSecond(UTC))
            .queryParam("to", to.toEpochSecond(UTC))
            .queryParam("countback", countback)
            .queryParam("currencyCode", currencyCode)
            .build()
            .encode() // Ensure encoding is applied
            .toUri();

    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
    headers.set("Accept", "*/*");
    headers.set("Referer", "https://www.moneycontrol.com/");
    headers.set("Origin", "https://www.moneycontrol.com");

    HttpEntity<String> entity = new HttpEntity<>(headers);
    // Make GET Request with Headers
    ResponseEntity<MCResult> response =
        restTemplate.exchange(uri, HttpMethod.GET, entity, MCResult.class);

    // Get the Response Body
    MCResult ohlc = response.getBody();

    List<StockOHLCV> ohlcvList = this.map(nseSymbol, ohlc);

    return ohlcvList;
  }

  private List<StockOHLCV> map(String symbol, MCResult ohlc) {

    List<StockOHLCV> ohlcvList = new ArrayList<>();

    List<Long> dates = ohlc.getT();

    List<Double> opens = ohlc.getO();

    List<Double> highs = ohlc.getH();

    List<Double> lows = ohlc.getL();

    List<Double> closes = ohlc.getC();

    List<Long> volumes = ohlc.getV();

    for (int i = 0; i < dates.size(); i++) {

      StockOHLCV ohlcv = new StockOHLCV();

      ohlcv.setSymbol(symbol);
      ohlcv.setOpen(BigDecimal.valueOf(opens.get(i)));
      ohlcv.setHigh(BigDecimal.valueOf(highs.get(i)));
      ohlcv.setLow(BigDecimal.valueOf(lows.get(i)));
      ohlcv.setClose(BigDecimal.valueOf(closes.get(i)));
      ohlcv.setVolume(volumes.get(i));
      ohlcv.setDate(Instant.ofEpochSecond(dates.get(i)).atZone(UTC).toLocalDate());
      ohlcvList.add(ohlcv);
    }

    return ohlcvList;
  }
}
