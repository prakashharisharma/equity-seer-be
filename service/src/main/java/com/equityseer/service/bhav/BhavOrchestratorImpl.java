package com.equityseer.service.bhav;

import com.equityseer.modal.Bhav;
import com.equityseer.service.BhavcopyService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BhavOrchestratorImpl implements BhavOrchestrator {
  private final BhavcopyService bhavcopyService;
  private final BhavProcessor bhavProcessor;

  @Override
  public void process(LocalDate sessionDate) throws IOException {
    Objects.requireNonNull(sessionDate, "sessionDate must not be null");
    log.info("Starting Bhav orchestration for sessionDate={}", sessionDate);

    List<Bhav> bhavList = bhavcopyService.downloadBhav(sessionDate);
    log.info("Downloaded {} Bhav rows for sessionDate={}", bhavList.size(), sessionDate);

    bhavProcessor.process(bhavList);
  }
}
