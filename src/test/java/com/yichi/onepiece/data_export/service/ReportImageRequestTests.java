package com.yichi.onepiece.data_export.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import java.io.ByteArrayInputStream;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.Test;

public class ReportImageRequestTests {
  @Test
  public void reportProcessingDoesNotAlterExistingThumbnailOrOriginalRequests() throws Exception {
    var client = mock(OSS.class);
    var previous = ReflectionTestUtils.getField(AliCloudObjectStorage.class, "ossClient");
    when(client.getObject(any(GetObjectRequest.class))).thenAnswer(invocation -> {
      var object = new OSSObject();
      object.setObjectContent(new ByteArrayInputStream(new byte[] {1}));
      return object;
    });
    ReflectionTestUtils.setField(AliCloudObjectStorage.class, "ossClient", client);
    String uri = "https://onepiece-battles.oss-cn-shanghai.aliyuncs.com/test-portrait";
    try {
      try (var report = AliCloudObjectStorage.getReportImageStream(uri);
          var thumbnail = AliCloudObjectStorage.getImageStream(uri, false);
          var original = AliCloudObjectStorage.getImageStream(uri, true)) {
        assertThat(report.read()).isEqualTo(1);
        assertThat(thumbnail.read()).isEqualTo(1);
        assertThat(original.read()).isEqualTo(1);
      }
      var requests = ArgumentCaptor.forClass(GetObjectRequest.class);
      verify(client, times(3)).getObject(requests.capture());
      assertThat(requests.getAllValues()).extracting(GetObjectRequest::getProcess)
          .containsExactly("image/resize,m_lfit,w_256,h_256/format,jpg/quality,Q_80", "image/resize,w_100", null);
    } finally {
      ReflectionTestUtils.setField(AliCloudObjectStorage.class, "ossClient", previous);
    }
  }
}
