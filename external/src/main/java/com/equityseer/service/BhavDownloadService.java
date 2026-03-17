package com.equityseer.service;

import java.io.IOException;

public interface BhavDownloadService {
  byte[] downloadFile(String url) throws IOException;
}
