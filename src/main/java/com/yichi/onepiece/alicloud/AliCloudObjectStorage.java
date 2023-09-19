package com.yichi.onepiece.alicloud;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyuncs.exceptions.ClientException;
import com.yichi.onepiece.exception.FileStorageException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AliCloudObjectStorage {
  private static final Logger logger = LoggerFactory.getLogger(AliCloudObjectStorage.class);
  private static final String OSS_EXTERNAL_ENDPOINT = "oss-cn-shanghai.aliyuncs.com";
  private static final String OSS_BUCKET_NAME = "onepiece-battles";
  private static final OSS ossClient;

  static {
    try {
      EnvironmentVariableCredentialsProvider provider =
          CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
      ossClient = new OSSClientBuilder().build(OSS_EXTERNAL_ENDPOINT, provider);
    } catch (ClientException ce) {
      try {
        throw toFileStorageException(ce);
      } catch (FileStorageException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static String putFileStream(ByteArrayInputStream content) throws FileStorageException {
    String key = UUID.randomUUID().toString();
    try {
      ossClient.putObject(OSS_BUCKET_NAME, key, content);
      return toOssUri(key);
    } catch (OSSException oe) {
      throw toFileStorageException(oe);
    } finally {
      if (ossClient != null) {
        ossClient.shutdown();
      }
    }
  }

  private static FileStorageException toFileStorageException(OSSException ossException) {
    return new FileStorageException(
        String.format(
            "OSS threw exception. Error code: %s Request ID: %s Host ID: %s Error message: %s",
            ossException.getErrorCode(),
            ossException.getRequestId(),
            ossException.getHostId(),
            ossException.getErrorMessage()),
        ossException);
  }

  private static FileStorageException toFileStorageException(ClientException clientException) {
    return new FileStorageException(
        String.format(
            "OSS threw client exception. Error code: %s Request ID: %s Error: %s",
            clientException.getErrCode(),
            clientException.getRequestId(),
            clientException.getMessage()),
        clientException);
  }


  public static InputStream getFileStream(String uri) throws FileStorageException {
    try{
      String key = extractFromUri(uri);
      OSSObject ossObject = ossClient.getObject(OSS_BUCKET_NAME, key);
      return ossObject.getObjectContent();
    } catch (OSSException oe) {
      throw toFileStorageException(oe);
    } finally {
      if (ossClient != null) {
        ossClient.shutdown();
      }
    }
  }

  private static String extractFromUri(String uri) throws FileStorageException {
    String prefix = getUriPrefix();
    if (uri.startsWith(prefix)) {
      return uri.substring(prefix.length());
    } else {
      throw new FileStorageException("Unsupported bucket URI: " + uri);
    }
  }

  public static List<OSSObjectSummary> list() throws FileStorageException {
    try{
      ObjectListing ossObjects = ossClient.listObjects(OSS_BUCKET_NAME);
      return ossObjects.getObjectSummaries();
    } catch (OSSException oe) {
      throw toFileStorageException(oe);
    } finally {
      if (ossClient != null) {
        ossClient.shutdown();
      }
    }
  }

  public static void delete(String objectName) throws FileStorageException {
    try{
      ossClient.deleteObject(OSS_BUCKET_NAME, objectName);
    } catch (OSSException oe) {
      throw  toFileStorageException(oe);
    } finally {
      if (ossClient != null) {
        ossClient.shutdown();
      }
    }
  }
  private static String getUriPrefix() {
    return String.format("https://%s.%s/", AliCloudObjectStorage.OSS_BUCKET_NAME, OSS_EXTERNAL_ENDPOINT);
  }

  private static String toOssUri(String key) {
    return getUriPrefix() + key;
  }
}
