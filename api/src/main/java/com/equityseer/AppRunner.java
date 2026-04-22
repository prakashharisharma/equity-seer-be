package com.equityseer;

import com.equityseer.entity.stock.Stock;
import com.equityseer.service.pricing.EntryPriceService;
import com.equityseer.service.pricing.StopLossService;
import com.equityseer.service.scanner.ScannerService;
import com.equityseer.service.scoring.ScoringService;
import com.equityseer.service.validation.ValidationService;
import com.equityseer.type.TimeFrame;
import java.time.LocalDate;
import java.time.YearMonth;
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

  @Autowired private ValidationService validationService;

  @Autowired private EntryPriceService entryPriceService;

  @Autowired private StopLossService stopLossService;

  @Override
  public void run(String... args) throws Exception {
    this.printStockList();
  }

  private void printStockList() {
    // LocalDate date = LocalDate.of(2024, 1, 31);
    int year = 2024;
    int month = 02;

    LocalDate date = YearMonth.of(year, month).atEndOfMonth();
    List<Stock> stockList =
        scannerService.scanVolumeExpansionWithPriceActionSignal(TimeFrame.MONTHLY, date);

    record ScoredStock(String symbol, double score, double entryPrice, double stopLoss) {}

    stockList.stream()
        .filter(s -> validationService.isValid(s.getSymbol(), TimeFrame.MONTHLY, date))
        .map(
            s -> {
              double score = scoringService.score(s.getSymbol(), TimeFrame.MONTHLY, date);
              return new ScoredStock(
                  s.getSymbol(),
                  score,
                  entryPriceService.calculate(s.getSymbol(), TimeFrame.MONTHLY, date, score),
                  stopLossService.calculate(s.getSymbol(), TimeFrame.MONTHLY, date));
            })
        .filter(s -> s.score() > 5.0)
        .sorted((a, b) -> Double.compare(b.score(), a.score()))
        .forEach(
            s ->
                System.out.printf(
                    "%s : %s : %.2f : Entry: %.2f : SL: %.2f%n",
                    date, s.symbol(), s.score(), s.entryPrice(), s.stopLoss()));
  }
}
