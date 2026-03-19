package com.equityseer.modal;

import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MCResult implements Serializable {

  String s;

  List<Long> t;

  List<Double> o;

  List<Double> h;

  List<Double> l;

  List<Double> c;

  List<Long> v;
}
