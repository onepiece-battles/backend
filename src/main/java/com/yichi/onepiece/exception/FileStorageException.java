package com.yichi.onepiece.exception;

public class FileStorageException extends Exception {
  private static final long serialVersionUID = 1L;

  public FileStorageException(String message, Throwable cause) {
    super(message, cause);
  }

  public FileStorageException(String message) {
    super(message);
  }
}
