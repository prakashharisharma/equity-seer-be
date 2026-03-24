package com.equityseer;

import com.equityseer.entity.stock.Stock;
import com.equityseer.service.scanner.ScannerService;
import com.equityseer.service.scoring.ScoringService;
import com.equityseer.type.TimeFrame;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AppRunner implements CommandLineRunner {

  @Autowired private ScannerService scannerService;

  @Autowired private ScoringService scoringService;

  @Override
  public void run(String... args) throws Exception {
    this.printStockList();
  }

  private void printStockList() {
    LocalDate date = LocalDate.of(2025, 3, 31);
    List<Stock> stockList =
        scannerService.scanVolumeExpansionWithPriceActionSignal(TimeFrame.MONTHLY, date);

    record ScoredStock(String symbol, double score) {}

    stockList.stream()
        .map(
            s ->
                new ScoredStock(
                    s.getSymbol(), scoringService.score(s.getSymbol(), TimeFrame.MONTHLY, date)))
        .sorted((a, b) -> Double.compare(b.score(), a.score()))
        .forEach(s -> System.out.println(s.symbol() + " : " + s.score()));
  }
}
