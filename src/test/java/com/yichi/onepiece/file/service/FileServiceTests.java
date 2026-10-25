package com.yichi.onepiece.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.file.model.FileObjectEntity;
import com.yichi.onepiece.user.UserHelper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.testng.annotations.Test;

public class FileServiceTests {
  @Test
  public void closesDownloadStreamOnSuccess() throws Exception {
    var stream = spy(new ByteArrayInputStream(new byte[] {1, 2, 3}));
    var fileId = UUID.randomUUID();
    var fileObjectService = mock(FileObjectService.class);
    var fileObject = mock(FileObjectEntity.class);
    when(fileObjectService.get(fileId)).thenReturn(fileObject);
    when(fileObject.getOssUri()).thenReturn("oss-test");
    when(fileObject.getFileName()).thenReturn("test.bin");
    try (var userHelper = mockStatic(UserHelper.class);
        var storage = mockStatic(AliCloudObjectStorage.class)) {
      userHelper.when(UserHelper::hasAdminPermission).thenReturn(true);
      storage.when(() -> AliCloudObjectStorage.getFileStream("oss-test")).thenReturn(stream);
      var response = new FileService(fileObjectService).downloadFile(fileId);
      assertThat(response.getFileData()).isEqualTo("AQID");
      assertThat(response.getFileName()).isEqualTo("test.bin");
      verify(stream).close();
    }
  }

  @Test
  public void closesDownloadStreamOnReadFailure() throws Exception {
    var stream = mock(InputStream.class);
    when(stream.readAllBytes()).thenThrow(new IOException("read failed"));
    var fileId = UUID.randomUUID();
    var fileObjectService = mock(FileObjectService.class);
    var fileObject = mock(FileObjectEntity.class);
    when(fileObjectService.get(fileId)).thenReturn(fileObject);
    when(fileObject.getOssUri()).thenReturn("oss-test");
    try (var userHelper = mockStatic(UserHelper.class);
        var storage = mockStatic(AliCloudObjectStorage.class)) {
      userHelper.when(UserHelper::hasAdminPermission).thenReturn(true);
      storage.when(() -> AliCloudObjectStorage.getFileStream("oss-test")).thenReturn(stream);
      assertThatThrownBy(() -> new FileService(fileObjectService).downloadFile(fileId))
          .isInstanceOf(RuntimeException.class).hasMessage("file retrieval failed");
      verify(stream).close();
    }
  }
}
