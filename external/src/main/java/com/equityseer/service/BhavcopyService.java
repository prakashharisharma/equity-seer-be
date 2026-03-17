package com.equityseer.service;

import com.equityseer.modal.Bhav;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface BhavcopyService {
  List<Bhav> downloadBhav(LocalDate sessionDate) throws IOException;
}
