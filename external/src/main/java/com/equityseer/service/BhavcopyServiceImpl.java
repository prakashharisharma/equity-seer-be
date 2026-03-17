package com.equityseer.service;

import com.equityseer.modal.Bhav;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class BhavcopyServiceImpl implements BhavcopyService {

  private static final String NSE_URL_TEMPLATE =
      "https://nsearchives.nseindia.com/content/cm/BhavCopy_NSE_CM_0_0_0_%s_F_0000.csv.zip";

  private final BhavDownloadService bhavDownloadService;

  @Override
  public List<Bhav> downloadBhav(LocalDate sessionDate) throws IOException {
    String formattedDate = sessionDate.format(DateTimeFormatter.BASIC_ISO_DATE);
    String fileUrl = String.format(NSE_URL_TEMPLATE, formattedDate);

    log.info("Downloading NSE Bhavcopy from: {}", fileUrl);

    byte[] zipData = bhavDownloadService.downloadFile(fileUrl);
    String csvContent = extractCsvFromZip(zipData);
    return parseCsv(csvContent);
  }

  private String extractCsvFromZip(byte[] zipData) throws IOException {
    log.info("Extracting CSV from ZIP...");
    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(zipData);
        ZipInputStream zipInputStream = new ZipInputStream(byteArrayInputStream);
        StringWriter writer = new StringWriter()) {

      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.getName().endsWith(".csv")) {
          try (BufferedReader reader =
              new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
              writer.write(line);
              writer.write('\n');
            }
          }
          return writer.toString();
        }
      }
    }
    throw new IOException("CSV file not found in ZIP");
  }

  private List<Bhav> parseCsv(String csvContent) throws IOException {
    log.info("Parsing CSV content...");
    try (Reader reader = new StringReader(csvContent)) {
      CsvToBean<Bhav> csvToBean =
          new CsvToBeanBuilder<Bhav>(reader)
              .withType(Bhav.class)
              .withIgnoreLeadingWhiteSpace(true)
              .withSeparator(',')
              .build();
      return csvToBean.parse();
    } catch (RuntimeException e) {
      throw new IOException("Failed to parse CSV content", e);
    }
  }
}
