package com.yichi.onepiece.alicloud;

import com.yichi.onepiece.exception.FileStorageException;
import java.io.InputStream;

public interface FileObjectStorage {
  String putFileStream(InputStream inputStream) throws FileStorageException;
  InputStream getFileStream(String uri) throws FileStorageException;
  String deleteFileStream(String uri);
}
