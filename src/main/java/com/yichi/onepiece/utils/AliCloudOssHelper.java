package com.yichi.onepiece.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyuncs.exceptions.ClientException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AliCloudOssHelper {
  private static final Logger logger = LoggerFactory.getLogger(AliCloudOssHelper.class);
  private static final String OSS_EXTERNAL_ENDPOINT = "oss-cn-shanghai.aliyuncs.com";
  private static final String OSS_BUCKET_NAME = "onepiece-battles";
  private static final OSS ossClient;

  static {
    try {
      EnvironmentVariableCredentialsProvider provider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
      ossClient = new OSSClientBuilder().build(OSS_EXTERNAL_ENDPOINT, provider);
    } catch (ClientException ce) {
      logger.error("Caught an ClientException, which means the client encountered "
          + "a serious internal problem while trying to communicate with OSS, "
          + "such as not being able to access the network.");
      logger.error("Error Message: {}", ce.getMessage());
      throw new RuntimeException(ce);
    }
  }

  public static void put(String objectName, ByteArrayInputStream content) {
    try{
      ossClient.putObject(OSS_BUCKET_NAME, objectName, content);
    } catch (OSSException oe) {
      logger.error("Caught an OSSException, which means your request made it to OSS, but was rejected with an error response for some reason.");
      logger.error("Error Message: {}" , oe.getErrorMessage());
      logger.error("Error Code: {}" , oe.getErrorCode());
      logger.error("Request ID: {}" , oe.getRequestId());
      logger.error("Host ID: {}" , oe.getHostId());
    } finally {
      if (ossClient != null) {
        ossClient.shutdown();
      }
    }
  }

  public static InputStream get(String objectName) {
    try{
      OSSObject ossObject = ossClient.getObject(OSS_BUCKET_NAME, objectName);
      return ossObject.getObjectContent();
    } catch (OSSException oe) {
      logger.error(
          "Caught an OSSException, which means your request made it to OSS, but was rejected with an error response for some reason.");
      logger.error("Error Message: {}" , oe.getErrorMessage());
      logger.error("Error Code: {}" , oe.getErrorCode());
      logger.error("Request ID: {}" , oe.getRequestId());
      logger.error("Host ID: {}" , oe.getHostId());
    } finally {
      if (ossClient != null) {
        ossClient.shutdown();
      }
    }
    // todo: need to confirm how the deal with null result.
    return null;
  }

  public static List<OSSObjectSummary> list() {
    try{
      ObjectListing ossObjects = ossClient.listObjects(OSS_BUCKET_NAME);
      return ossObjects.getObjectSummaries();
    } catch (OSSException oe) {
      logger.error(
          "Caught an OSSException, which means your request made it to OSS, but was rejected with an error response for some reason.");
      logger.error("Error Message: {}" , oe.getErrorMessage());
      logger.error("Error Code: {}" , oe.getErrorCode());
      logger.error("Request ID: {}" , oe.getRequestId());
      logger.error("Host ID: {}" , oe.getHostId());
    } finally {
      if (ossClient != null) {
        ossClient.shutdown();
      }
    }
    // todo: need to confirm how the deal with null result.
    return null;
  }

  public static void delete(String objectName) {
    try{
      ossClient.deleteObject(OSS_BUCKET_NAME, objectName);
    } catch (OSSException oe) {
      logger.error(
          "Caught an OSSException, which means your request made it to OSS, but was rejected with an error response for some reason.");
      logger.error("Error Message: {}" , oe.getErrorMessage());
      logger.error("Error Code: {}" , oe.getErrorCode());
      logger.error("Request ID: {}" , oe.getRequestId());
      logger.error("Host ID: {}" , oe.getHostId());
    } finally {
      if (ossClient != null) {
        ossClient.shutdown();
      }
    }
  }
}
