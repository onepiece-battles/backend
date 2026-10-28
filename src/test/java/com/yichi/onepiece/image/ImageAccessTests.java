package com.yichi.onepiece.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.exception.FileStorageException;
import com.yichi.onepiece.image.controller.ImageAccessController;
import com.yichi.onepiece.image.dto.ImageAccess;
import com.yichi.onepiece.image.service.ImageService;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testng.annotations.Test;

public class ImageAccessTests {
  private static final String URI = "https://onepiece-battles.oss-cn-shanghai.aliyuncs.com/image-id";

  @Test
  public void resolvesShortLivedThumbnailAndOriginalWithoutDownloadingImage() throws Exception {
    var client = mock(OSS.class);
    var previous = ReflectionTestUtils.getField(AliCloudObjectStorage.class, "ossClient");
    when(client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
        .thenAnswer(invocation -> new URL(URI +
            (((GeneratePresignedUrlRequest) invocation.getArgument(0)).getProcess() == null
                ? "?original" : "?thumbnail")));
    ReflectionTestUtils.setField(AliCloudObjectStorage.class, "ossClient", client);
    try {
      var service = new ImageService(null, null, null, null, null, null);
      long before = System.currentTimeMillis();
      var access = service.getImageAccess(URI, false);
      assertThat(access.url()).endsWith("?thumbnail");
      assertThat(access.originalUrl()).endsWith("?original");
      assertThat(access.expiresAt()).isBetween(before + 14 * 60_000, before + 16 * 60_000);
      var calls = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
      verify(client, times(2)).generatePresignedUrl(calls.capture());
      assertThat(calls.getAllValues()).extracting(GeneratePresignedUrlRequest::getKey)
          .containsExactly("image-id", "image-id");
      assertThat(calls.getAllValues()).extracting(GeneratePresignedUrlRequest::getProcess)
          .containsExactly(null, "image/resize,w_100");
      assertThat(calls.getAllValues()).allSatisfy(request -> {
        assertThat(request.getMethod()).isEqualTo(com.aliyun.oss.HttpMethod.GET);
        assertThat(request.getExpiration().getTime()).isEqualTo(access.expiresAt());
      });
      verifyNoMoreInteractions(client);
    } finally {
      ReflectionTestUtils.setField(AliCloudObjectStorage.class, "ossClient", previous);
    }
  }

  @Test
  public void signedUrlsUseHttpsAndIncludeProcessingOnlyForThumbnails() throws Exception {
    var client = new OSSClientBuilder().build(
        "https://oss-cn-shanghai.aliyuncs.com", "test-access", "test-secret");
    var previous = ReflectionTestUtils.getField(AliCloudObjectStorage.class, "ossClient");
    ReflectionTestUtils.setField(AliCloudObjectStorage.class, "ossClient", client);
    try {
      var expiration = new Date(System.currentTimeMillis() + 60_000);
      var thumbnail = AliCloudObjectStorage.signImageUrl(URI, false, expiration);
      var original = AliCloudObjectStorage.signImageUrl(URI, true, expiration);
      assertThat(original.getProtocol()).isEqualTo("https");
      assertThat(original.getHost()).isEqualTo("onepiece-battles.oss-cn-shanghai.aliyuncs.com");
      assertThat(original.getQuery()).contains("Signature=").doesNotContain("x-oss-process");
      assertThat(URLDecoder.decode(thumbnail.getQuery(), StandardCharsets.UTF_8))
          .contains("x-oss-process=image/resize,w_100");
      for (var invalid : new String[] {"https://example.com/image", URI + "?query", URI + "#fragment",
          "https://onepiece-battles.oss-cn-shanghai.aliyuncs.com/"}) {
        assertThatThrownBy(() -> AliCloudObjectStorage.signImageUrl(invalid, true, expiration))
            .isInstanceOf(FileStorageException.class);
      }
    } finally {
      ReflectionTestUtils.setField(AliCloudObjectStorage.class, "ossClient", previous);
      client.shutdown();
    }
  }

  @Test
  public void endpointReturnsOnlyNoStoreMetadataAndRejectsInvalidUris() throws Exception {
    var service = mock(ImageService.class);
    when(service.getImageAccess(URI, true)).thenReturn(new ImageAccess("https://example.test/full",
        "https://example.test/full", 123456789));
    when(service.getImageAccess("invalid", false)).thenThrow(new FileStorageException("invalid URI"));
    var mvc = MockMvcBuilders.standaloneSetup(new ImageAccessController(service)).build();
    mvc.perform(post("/api/onepiece/image/get_image_url").contentType(MediaType.APPLICATION_JSON)
            .content("{\"uri\":\"" + URI + "\",\"full_image\":true}"))
        .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.original_url").value("https://example.test/full"))
        .andExpect(jsonPath("$.expires_at").value(123456789))
        .andExpect(jsonPath("$.image").doesNotExist());
    mvc.perform(post("/api/onepiece/image/get_image_url").contentType(MediaType.APPLICATION_JSON)
            .content("{\"uri\":\"invalid\",\"full_image\":false}"))
        .andExpect(status().isBadRequest());
  }
}
