package com.equityseer;

import com.equityseer.entity.stock.Stock;
import com.equityseer.service.pricing.EntryPriceService;
import com.equityseer.service.pricing.StopLossService;
import com.equityseer.service.pricing.TargetService;
import com.equityseer.service.scanner.ScannerService;
import com.equityseer.service.scoring.ScoringService;
import com.equityseer.service.validation.ValidationService;
import com.equityseer.type.TimeFrame;
import com.equityseer.util.MathUtils;
import java.time.LocalDate;
import java.time.Month;
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

  @Autowired private TargetService targetService;

  @Override
  public void run(String... args) throws Exception {
    this.printStockList();
  }

  private void printStockList() {
    // LocalDate date = LocalDate.of(2024, 1, 31);
    int year = 2022;
    int month = Month.JANUARY.getValue();

    LocalDate date = YearMonth.of(year, month).atEndOfMonth();

    List<Stock> stockList =
        scannerService.scanVolumeExpansionWithPriceActionSignal(TimeFrame.MONTHLY, date);

    record ScoredStock(
        String symbol,
        double score,
        double entryPrice,
        double stopLoss,
        double target,
        double targetPct) {}

    stockList.stream()
        .filter(s -> validationService.isValid(s.getSymbol(), TimeFrame.MONTHLY, date))
        .map(
            s -> {
              double score = scoringService.score(s.getSymbol(), TimeFrame.MONTHLY, date);
              double entry =
                  entryPriceService.calculate(s.getSymbol(), TimeFrame.MONTHLY, date, score);
              double target =
                  targetService.calculate(s.getSymbol(), TimeFrame.MONTHLY, date, entry);
              return new ScoredStock(
                  s.getSymbol(),
                  score,
                  entry,
                  stopLossService.calculate(s.getSymbol(), TimeFrame.MONTHLY, date),
                  target,
                  MathUtils.calculatePercentageChange(entry, target));
            })
        .filter(s -> s.score() > 5.0)
        .sorted((a, b) -> Double.compare(b.score(), a.score()))
        .forEach(
            s ->
                System.out.printf(
                    "%s : %s : %.2f : Entry: %.2f : SL: %.2f : Target: %.2f (%.2f%%)%n",
                    date,
                    s.symbol(),
                    s.score(),
                    s.entryPrice(),
                    s.stopLoss(),
                    s.target(),
                    s.targetPct()));
  }
}
/*
1. not perfectly align 200 > 100 > 50 || 200 < 100 < 50  && 100 or 200 decreasing
2. Strong resisstance past 5 years upper wick > 20% of body and Max(open, close) < High
&& min of 5 years high  (> min of max(open,close))<= other highs

3. 20 and 50 decreasing and vol > 2.5 * avg -5
4. 20 or 50 decreasing and vol > 2.5 * avg -5
5. if 100 & 200 decreasing and close on top -5

6. if 200 & 100 & 50 == 0 -5

7. if 50 > 200 &&  and 4 increasing then don't check above


20 > 50 > 200 or 5 > 20 > 50 && close > 200 or all increasing

-- If daily close above Target then target to be calculate again

 */
