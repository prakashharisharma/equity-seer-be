package com.equityseer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.equityseer.modal.Bhav;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BhavcopyServiceImplTest {
  @Mock private BhavDownloadService bhavDownloadService;

  @InjectMocks private BhavcopyServiceImpl bhavcopyService;

  private static final String HEADER =
      String.join(
          "",
          "TradDt,BizDt,Sgmt,Src,FinInstrmTp,FinInstrmId,ISIN,TckrSymb,SctySrs,",
          "XpryDt,FininstrmActlXpryDt,StrkPric,OptnTp,FinInstrmNm,OpnPric,HghPric,",
          "LwPric,ClsPric,LastPric,PrvsClsgPric,UndrlygPric,SttlmPric,OpnIntrst,",
          "ChngInOpnIntrst,TtlTradgVol,TtlTrfVal,TtlNbOfTxsExctd,SsnId,NewBrdLotQty,",
          "Rmks,Rsvd1,Rsvd2,Rsvd3,Rsvd4");

  @Test
  void downloadBhav_downloadsZipAndParsesCsv() throws Exception {
    LocalDate sessionDate = LocalDate.of(2026, 3, 17);
    String expectedUrl =
        "https://nsearchives.nseindia.com/content/cm/BhavCopy_NSE_CM_0_0_0_20260317_F_0000.csv.zip";

    String row = sampleRow();
    String csv = String.join("\n", HEADER, row);

    byte[] zipBytes = zipWithSingleCsv("bhav.csv", csv);

    when(bhavDownloadService.downloadFile(expectedUrl)).thenReturn(zipBytes);

    List<Bhav> result = bhavcopyService.downloadBhav(sessionDate);

    verify(bhavDownloadService).downloadFile(expectedUrl);
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().getNseSymbol()).isEqualTo("TCS");
    assertThat(result.getFirst().getOpen()).isEqualTo(100.0);
    assertThat(result.getFirst().getHigh()).isEqualTo(110.0);
    assertThat(result.getFirst().getLow()).isEqualTo(90.0);
    assertThat(result.getFirst().getClose()).isEqualTo(105.0);
    assertThat(result.getFirst().getTottrdqty()).isEqualTo(1000L);
  }

  @Test
  void downloadBhav_throwsWhenZipHasNoCsv() throws Exception {
    LocalDate sessionDate = LocalDate.of(2026, 3, 17);
    String expectedUrl =
        "https://nsearchives.nseindia.com/content/cm/BhavCopy_NSE_CM_0_0_0_20260317_F_0000.csv.zip";

    byte[] zipBytes = zipWithSingleEntry("not-a-csv.txt", "hello");
    when(bhavDownloadService.downloadFile(expectedUrl)).thenReturn(zipBytes);

    assertThatThrownBy(() -> bhavcopyService.downloadBhav(sessionDate))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("CSV file not found");
  }

  private static byte[] zipWithSingleCsv(String name, String csv) throws IOException {
    return zipWithSingleEntry(name, csv);
  }

  private static byte[] zipWithSingleEntry(String name, String content) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
      zos.putNextEntry(new ZipEntry(name));
      zos.write(content.getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }
    return baos.toByteArray();
  }

  private static String sampleRow() {
    return String.join(
        ",",
        "20260317", // TradDt
        "20260317", // BizDt
        "CM", // Sgmt
        "NSE", // Src
        "EQ", // FinInstrmTp
        "123", // FinInstrmId
        "INE123456789", // ISIN
        "TCS", // TckrSymb
        "EQ", // SctySrs
        "", // XpryDt
        "", // FininstrmActlXpryDt
        "", // StrkPric
        "", // OptnTp
        "TATA CONSULTANCY SERVICES", // FinInstrmNm
        "100.0", // OpnPric
        "110.0", // HghPric
        "90.0", // LwPric
        "105.0", // ClsPric
        "105.0", // LastPric
        "99.0", // PrvsClsgPric
        "", // UndrlygPric
        "", // SttlmPric
        "", // OpnIntrst
        "", // ChngInOpnIntrst
        "1000", // TtlTradgVol
        "12345.6", // TtlTrfVal
        "10", // TtlNbOfTxsExctd
        "", // SsnId
        "", // NewBrdLotQty
        "remarks", // Rmks
        "", // Rsvd1
        "", // Rsvd2
        "", // Rsvd3
        ""); // Rsvd4
  }
}
