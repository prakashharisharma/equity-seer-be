package com.equityseer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BhavDownloadServiceImplTest {

  @Test
  void downloadFile_returnsBytesWhenHttpOk() throws Exception {
    byte[] expected = "hello".getBytes(StandardCharsets.UTF_8);

    HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
    Mockito.when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    Mockito.when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(expected));

    BhavDownloadServiceImpl service =
        new BhavDownloadServiceImpl() {
          @Override
          HttpURLConnection openConnection(String fileUrl) {
            return connection;
          }
        };

    byte[] actual = service.downloadFile("https://example.com/file.zip");

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void downloadFile_throwsWhenNon200() throws Exception {
    HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
    Mockito.when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);

    BhavDownloadServiceImpl service =
        new BhavDownloadServiceImpl() {
          @Override
          HttpURLConnection openConnection(String fileUrl) {
            return connection;
          }
        };

    assertThatThrownBy(() -> service.downloadFile("https://example.com/file.zip"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("HTTP response code");
  }

  @Test
  void recoverDownloadFile_returnsEmptyArray() {
    BhavDownloadServiceImpl service = new BhavDownloadServiceImpl();
    byte[] result =
        service.recoverDownloadFile(new IOException("boom"), "https://example.com/file");
    assertThat(result).isEmpty();
  }

  @Test
  void downloadFile_setsExpectedHeadersAndTimeouts() throws Exception {
    HttpURLConnection connection = Mockito.mock(HttpURLConnection.class);
    Mockito.when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
    Mockito.when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    AtomicInteger connectTimeout = new AtomicInteger();
    AtomicInteger readTimeout = new AtomicInteger();

    Mockito.doAnswer(
            inv -> {
              connectTimeout.set(inv.getArgument(0));
              return null;
            })
        .when(connection)
        .setConnectTimeout(Mockito.anyInt());

    Mockito.doAnswer(
            inv -> {
              readTimeout.set(inv.getArgument(0));
              return null;
            })
        .when(connection)
        .setReadTimeout(Mockito.anyInt());

    BhavDownloadServiceImpl service =
        new BhavDownloadServiceImpl() {
          @Override
          HttpURLConnection openConnection(String fileUrl) {
            return connection;
          }
        };

    service.downloadFile("https://example.com/file.zip");

    Mockito.verify(connection).setRequestMethod("GET");
    Mockito.verify(connection)
        .setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
    Mockito.verify(connection).setRequestProperty("Accept", "*/*");
    Mockito.verify(connection).setRequestProperty("Accept-Encoding", "gzip, deflate, br");
    Mockito.verify(connection).setRequestProperty("Accept-Language", "en-US,en;q=0.9");
    Mockito.verify(connection).setRequestProperty("Referer", "https://www.nseindia.com/");
    assertThat(connectTimeout.get()).isEqualTo(10_000);
    assertThat(readTimeout.get()).isEqualTo(10_000);
  }
}
