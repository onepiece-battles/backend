package com.yichi.onepiece.Exception;

import com.google.common.base.Strings;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import org.springframework.security.access.AuthorizationServiceException;

public class ExceptionUtils {
  public ExceptionUtils() {}

  public static Supplier<NoSuchElementException> notFound(String message) {
    return () -> new NoSuchElementException(message);
  }

  public static Supplier<NoSuchElementException> notFound(String baseMessage, Object... args) {
    return () -> new NoSuchElementException(Strings.lenientFormat(baseMessage, args));
  }

  public static AuthorizationServiceException emptyAuthority(String baseMessage) {
    return new AuthorizationServiceException(baseMessage);
  }

  public static AuthorizationServiceException unexpectedAuthorityType(String baseMessage) {
    return new AuthorizationServiceException(baseMessage);
  }
}
