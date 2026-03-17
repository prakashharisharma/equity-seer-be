package com.equityseer.service.bhav;

import java.io.IOException;
import java.time.LocalDate;

public interface BhavOrchestrator {
  void process(LocalDate sessionDate) throws IOException;
}
