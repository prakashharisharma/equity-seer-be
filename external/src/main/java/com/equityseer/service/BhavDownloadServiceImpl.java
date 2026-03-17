package com.equityseer.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BhavDownloadServiceImpl implements BhavDownloadService {

  @Retryable(
      retryFor = {Exception.class},
      maxAttempts = 5, // initial attempt + 4 retries
      backoff =
          @Backoff(
              delay = 900_000,
              multiplier = 2,
              maxDelay = 7_200_000)) // 15 min, 30 min, 60 min...
  public byte[] downloadFile(String fileUrl) throws IOException {

    log.info("Downloading file from: {}", fileUrl);
    HttpURLConnection connection = openConnection(fileUrl);
    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
    connection.setRequestProperty("Accept", "*/*");
    connection.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
    connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
    connection.setRequestProperty("Referer", "https://www.nseindia.com/");

    connection.setRequestMethod("GET");
    connection.setConnectTimeout(10_000);
    connection.setReadTimeout(10_000);

    int responseCode = connection.getResponseCode();

    if (responseCode != HttpURLConnection.HTTP_OK) {
      throw new IOException("Failed to download file, HTTP response code: " + responseCode);
    }

    try (InputStream inputStream = connection.getInputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
      return outputStream.toByteArray();
    }
  }

  HttpURLConnection openConnection(String fileUrl) throws IOException {
    return (HttpURLConnection) new URL(fileUrl).openConnection();
  }

  /** Called when all retries are exhausted. */
  @Recover
  public byte[] recoverDownloadFile(IOException ex, String fileUrl) {
    log.error("Failed to download file after multiple retries: {}", fileUrl, ex);
    return new byte[0];
  }
}
