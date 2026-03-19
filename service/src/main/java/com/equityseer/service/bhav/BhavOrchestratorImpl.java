package com.equityseer.service.bhav;

import com.equityseer.entity.stock.Stock;
import com.equityseer.entity.stock.StockOHLCV;
import com.equityseer.modal.Bhav;
import com.equityseer.repository.stock.StockOHLCVRepository;
import com.equityseer.repository.stock.StockRepository;
import com.equityseer.service.BhavcopyService;
import com.equityseer.service.McService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BhavOrchestratorImpl implements BhavOrchestrator {
  private final BhavcopyService bhavcopyService;
  private final BhavProcessor bhavProcessor;
  private final StockRepository stockRepository;
  private final StockOHLCVRepository stockOHLCVRepository;
  private final McService mcService;
  private final EntityManager entityManager;

  @Override
  public void process(LocalDate sessionDate) throws IOException {
    Objects.requireNonNull(sessionDate, "sessionDate must not be null");
    log.info("Starting Bhav orchestration for sessionDate={}", sessionDate);

    List<Bhav> bhavList = bhavcopyService.downloadBhav(sessionDate);
    log.info("Downloaded {} Bhav rows for sessionDate={}", bhavList.size(), sessionDate);

    List<Bhav> filteredBhavList =
        bhavList.stream()
            .filter(bhav -> List.of("EQ", "BE", "SM", "BZ", "Z", "PZ").contains(bhav.getSeries()))
            .toList();
    log.info(
        "Filtered to {} Bhav rows with series EQ, BE, SM, BZ, Z, PZ for sessionDate={}",
        filteredBhavList.size(),
        sessionDate);

    bhavProcessor.process(filteredBhavList);
  }

  @Override
  @Async
  public void processHistorical() {
    log.info("Starting historical data processing");

    List<Stock> stocks = stockRepository.findByActivityCompletedFalse();
    log.info("Found {} stocks to process", stocks.size());

    for (Stock stock : stocks) {
      try {
        processStockHistorical(stock);
      } catch (Exception e) {
        log.error("Failed to process historical data for stock: {}", stock.getSymbol(), e);
      }
    }

    log.info("Completed historical data processing");
  }

  @Transactional
  protected void processStockHistorical(Stock stock) {
    log.info("Starting historical data processing for stock: {}", stock.getSymbol());

    try {
      // Fetch OHLCV data
      List<StockOHLCV> ohlcvs = mcService.getOHLCV(stock.getSymbol(), 30, 7350);

      boolean hasData = ohlcvs != null && !ohlcvs.isEmpty();

      if (hasData) {
        // Delete existing OHLCV records
        int deletedCount = stockOHLCVRepository.deleteBySymbol(stock.getSymbol());
        log.debug(
            "Deleted {} existing OHLCV records for stock: {}", deletedCount, stock.getSymbol());

        // Insert new OHLCV records
        List<StockOHLCV> savedOhlcvs = stockOHLCVRepository.saveAll(ohlcvs);
        log.info("Inserted {} OHLCV records for stock: {}", savedOhlcvs.size(), stock.getSymbol());
      } else {
        log.warn("No OHLCV data received for stock: {} - marking as completed", stock.getSymbol());
        // For empty/null data, we still mark as completed since there's no data to process
      }

      // Add random delay between 500ms to 3000ms
      long delay = ThreadLocalRandom.current().nextLong(500, 3001);
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Thread interrupted during sleep for stock: {}", stock.getSymbol());
      }

      // Mark stock as completed only after all operations succeed
      stock.setActivityCompleted(true);
      stockRepository.save(stock);
      log.info(
          "Successfully completed historical data processing for stock: {}", stock.getSymbol());

      // Clear persistence context to avoid memory bloat
      entityManager.clear();

    } catch (Exception e) {
      log.error("Error processing historical data for stock: {}", stock.getSymbol(), e);
      throw e; // Re-throw to trigger transaction rollback
    }
  }
}
