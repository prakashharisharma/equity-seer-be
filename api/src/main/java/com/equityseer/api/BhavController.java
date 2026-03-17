package com.equityseer.api;

import com.equityseer.service.bhav.BhavOrchestrator;
import java.io.IOException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/bhav")
@RequiredArgsConstructor
public class BhavController {
  private final BhavOrchestrator bhavOrchestrator;

  @PostMapping("/process")
  public ResponseEntity<Void> process(
      @RequestParam("sessionDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate sessionDate)
      throws IOException {
    log.info("Received bhav process request sessionDate={}", sessionDate);
    bhavOrchestrator.process(sessionDate);
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }
}
