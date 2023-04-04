package com.yichi.onepiece.Exception;

import com.google.common.base.Strings;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class ExceptionUtils {
    public ExceptionUtils() {
    }

    public static Supplier<NoSuchElementException> notFound(String message) {
        return () -> new NoSuchElementException(message);
    }

    public static Supplier<NoSuchElementException> notFound(String baseMessage, Object... args) {
        return () -> new NoSuchElementException(Strings.lenientFormat(baseMessage, args));
    }
}
