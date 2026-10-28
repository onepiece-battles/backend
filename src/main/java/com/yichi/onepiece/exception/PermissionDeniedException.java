package com.yichi.onepiece.exception;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
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
