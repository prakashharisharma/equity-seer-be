package com.equityseer;

import com.equityseer.service.scanner.ScannerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AppRunner implements CommandLineRunner {

  @Autowired private ScannerService scannerService;

  @Override
  public void run(String... args) throws Exception {
    /*
    List<Stock> stockList =
        scannerService.scanEmaAlignmentWithMomentum(TimeFrame.MONTHLY, LocalDate.of(2026, 02, 28));
    stockList.forEach(
        s -> {
          System.out.println(s.getSymbol());
        });
        */
  }
}
