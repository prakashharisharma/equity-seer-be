package com.equityseer;

import com.equityseer.service.McService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AppRunner implements CommandLineRunner {

  @Autowired private McService mcService;

  @Override
  public void run(String... args) throws Exception {

    // List<StockOHLCV> stockOHLCVList = mcService.getOHLCV("TCS", 30, 7350);

    // stockOHLCVList.forEach(System.out::println);
  }
}
