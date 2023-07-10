package com.yichi.onepiece.Exception;

import java.io.Serial;

public class PermissionDeniedException extends RuntimeException {
  @Serial private static final long serialVersionUID = -5365630128856068166L;

  public PermissionDeniedException() {
    super();
  }

  public PermissionDeniedException(String s) {
    super(s);
  }

  public PermissionDeniedException(Throwable cause) {
    super();
  }
}
