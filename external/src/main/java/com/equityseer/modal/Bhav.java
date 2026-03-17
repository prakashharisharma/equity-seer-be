package com.equityseer.modal;

import com.opencsv.bean.CsvBindByName;
import java.io.Serializable;
import lombok.Data;

@Data
public class Bhav implements Serializable {

  private static final long serialVersionUID = -7916644484544527519L;

  @CsvBindByName(column = "TradDt")
  private String timestamp;

  @CsvBindByName(column = "BizDt")
  private String bizDt;

  @CsvBindByName(column = "Sgmt")
  private String segment;

  @CsvBindByName(column = "Src")
  private String source;

  @CsvBindByName(column = "FinInstrmTp")
  private String finInstrmTp;

  @CsvBindByName(column = "FinInstrmId")
  private String exchangeCode;

  @CsvBindByName(column = "ISIN")
  private String isin;

  @CsvBindByName(column = "TckrSymb")
  private String nseSymbol;

  @CsvBindByName(column = "SctySrs")
  private String series;

  @CsvBindByName(column = "XpryDt")
  private String expiryDate;

  @CsvBindByName(column = "FininstrmActlXpryDt")
  private String fininstrmActlXpryDt;

  @CsvBindByName(column = "StrkPric")
  private Double strkPric;

  @CsvBindByName(column = "OptnTp")
  private String optnTp;

  @CsvBindByName(column = "FinInstrmNm")
  private String companyName;

  @CsvBindByName(column = "OpnPric")
  private Double open;

  @CsvBindByName(column = "HghPric")
  private Double high;

  @CsvBindByName(column = "LwPric")
  private Double low;

  @CsvBindByName(column = "ClsPric")
  private Double close;

  @CsvBindByName(column = "LastPric")
  private Double last;

  @CsvBindByName(column = "PrvsClsgPric")
  private Double prevClose;

  @CsvBindByName(column = "UndrlygPric")
  private Double undrlygPric;

  @CsvBindByName(column = "SttlmPric")
  private Double sttlmPric;

  @CsvBindByName(column = "OpnIntrst")
  private String opnIntrst;

  @CsvBindByName(column = "ChngInOpnIntrst")
  private String chngInOpnIntrst;

  @CsvBindByName(column = "TtlTradgVol")
  private Long tottrdqty;

  @CsvBindByName(column = "TtlTrfVal")
  private Double tottrdval;

  @CsvBindByName(column = "TtlNbOfTxsExctd")
  private Long totaltrades;

  @CsvBindByName(column = "SsnId")
  private String ssnId;

  @CsvBindByName(column = "NewBrdLotQty")
  private Long newBrdLotQty;

  @CsvBindByName(column = "Rmks")
  private String remarks;

  @CsvBindByName(column = "Rsvd1")
  private String rsvd1;

  @CsvBindByName(column = "Rsvd2")
  private String rsvd2;

  @CsvBindByName(column = "Rsvd3")
  private String rsvd3;

  @CsvBindByName(column = "Rsvd4")
  private String rsvd4;
}
