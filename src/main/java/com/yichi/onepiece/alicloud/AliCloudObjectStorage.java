package com.yichi.onepiece.alicloud;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.ObjectListing;
import com.aliyuncs.exceptions.ClientException;
import com.yichi.onepiece.exception.FileStorageException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class AliCloudObjectStorage {
  private static final Logger logger = LoggerFactory.getLogger(AliCloudObjectStorage.class);
  private static final String OSS_EXTERNAL_ENDPOINT = "oss-cn-shanghai.aliyuncs.com";
  private static final String OSS_BUCKET_NAME = "onepiece-battles";
  private static final String OSS_ACCESS_KEY_ID_ENV = "ALIYUN_OSS_ACCESS_KEY_ID";
  private static final String OSS_ACCESS_KEY_SECRET_ENV = "ALIYUN_OSS_ACCESS_KEY_SECRET";

  private static volatile OSS ossClient;

  /**
   * Builds the OSS client lazily on first use. Credentials come exclusively from the environment and
   * must never be hardcoded or committed to source control (they are supplied at runtime, e.g. via
   * the deployment's environment variables). Building lazily lets the application and tests start
   * without OSS credentials — they are only required when object storage is actually accessed.
   */
  private static OSS client() {
    OSS local = ossClient;
    if (local == null) {
      synchronized (AliCloudObjectStorage.class) {
        local = ossClient;
        if (local == null) {
          local =
              new OSSClientBuilder()
                  .build(
                      "https://" + OSS_EXTERNAL_ENDPOINT,
                      requireEnv(OSS_ACCESS_KEY_ID_ENV),
                      requireEnv(OSS_ACCESS_KEY_SECRET_ENV));
          ossClient = local;
        }
      }
    }
    return local;
  }

  private static String requireEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          "Environment variable "
              + name
              + " is not set. OSS credentials must be provided via the environment.");
    }
    return value;
  }

  public static String putFileStream(InputStream content) throws FileStorageException {
    return putFileStream(content, null);
  }

  public static String putFileStream(InputStream content, long contentLength)
      throws FileStorageException {
    return putFileStream(content, Long.valueOf(contentLength));
  }

  private static String putFileStream(InputStream content, Long contentLength)
      throws FileStorageException {
    String key = UUID.randomUUID().toString();
    try (content) {
      logger.info("start putting file to alicloud");
      if (contentLength == null) {
        client().putObject(OSS_BUCKET_NAME, key, content);
      } else {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        client().putObject(OSS_BUCKET_NAME, key, content, metadata);
      }
      return toOssUri(key);
    } catch (OSSException oe) {
      logger.info(oe.toString());
      throw toFileStorageException(oe);
    } catch (IOException ioe) {
      throw new FileStorageException("Failed to close uploaded file stream", ioe);
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
      logger.info("uri is: {}", uri);
      String key = extractFromUri(uri);
      logger.info("key is: {}", key);
      OSSObject ossObject = client().getObject(OSS_BUCKET_NAME, key);
      return ossObject.getObjectContent();
    } catch (OSSException oe) {
      throw toFileStorageException(oe);
    }
  }

  public static InputStream getImageStream(String uri, @Nullable Boolean fullImage) throws FileStorageException {
    return getProcessedImageStream(uri, fullImage == null || !fullImage ? "image/resize,w_100" : null);
  }

  /** Signs a read for one existing object; image bytes never pass through the app server. */
  public static URL signImageUrl(String uri, boolean fullImage, Date expiration)
      throws FileStorageException {
    if (uri == null || uri.indexOf('?') >= 0 || uri.indexOf('#') >= 0) {
      throw new FileStorageException("Expected a stored OSS image URI");
    }
    String key = extractFromUri(uri);
    if (key.isBlank()) throw new FileStorageException("Image object key is empty");
    var request = new GeneratePresignedUrlRequest(OSS_BUCKET_NAME, key, com.aliyun.oss.HttpMethod.GET);
    request.setExpiration(expiration);
    if (!fullImage) request.setProcess("image/resize,w_100");
    return client().generatePresignedUrl(request);
  }

  public static InputStream getReportImageStream(String uri) throws FileStorageException {
    return getProcessedImageStream(uri, "image/resize,m_lfit,w_256,h_256/format,jpg/quality,Q_80");
  }

  private static InputStream getProcessedImageStream(String uri, String process) throws FileStorageException {
    try{
      logger.info("uri is: {}", uri);
      String key = extractFromUri(uri);
      logger.info("key is: {}", key);
      OSSObject ossObject;
      GetObjectRequest request = new GetObjectRequest(OSS_BUCKET_NAME, key);
      if (process != null) {
          request.setProcess(process);
      }
      ossObject = client().getObject(request);
      return ossObject.getObjectContent();
    } catch (OSSException oe) {
      throw toFileStorageException(oe);
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
      ObjectListing ossObjects = client().listObjects(OSS_BUCKET_NAME);
      return ossObjects.getObjectSummaries();
    } catch (OSSException oe) {
      throw toFileStorageException(oe);
    }
  }

  public static void delete(String objectName) throws FileStorageException {
    try{
      client().deleteObject(OSS_BUCKET_NAME, objectName);
    } catch (OSSException oe) {
      throw  toFileStorageException(oe);
    }
  }
  private static String getUriPrefix() {
    return String.format("https://%s.%s/", AliCloudObjectStorage.OSS_BUCKET_NAME, OSS_EXTERNAL_ENDPOINT);
  }

  private static String toOssUri(String key) {
    return getUriPrefix() + key;
  }
}
