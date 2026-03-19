package com.equityseer.service;

import com.equityseer.entity.stock.StockOHLCV;
import java.util.List;

public interface McService {

  public List<StockOHLCV> getOHLCV(String nseSymbol, int years, int countback);
}
