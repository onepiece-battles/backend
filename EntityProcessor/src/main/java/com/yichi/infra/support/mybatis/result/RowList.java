package com.yichi.infra.support.mybatis.result;

import java.util.ArrayList;
import java.util.Collection;

public class RowList<T> extends ArrayList<T> {
  static final long serialVersionUID = 1L;

  public RowList() {}

  public static <T> RowList<T> of(Collection<T> source) {
    RowList<T> result = new RowList();
    result.addAll(source);
    return result;
  }
}
