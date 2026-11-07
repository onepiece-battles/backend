package com.yichi.onepiece.file.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.file.model.FileObjectEntity;
import com.yichi.onepiece.user.UserHelper;
import java.net.URI;
import java.util.Date;
import java.util.UUID;
import org.testng.annotations.Test;

public class FileServiceTests {
  @Test
  public void signsAReadOnlyUrlForAnAdminWithoutBufferingBytes() throws Exception {
    var fileId = UUID.randomUUID();
    var fileObjectService = mock(FileObjectService.class);
    var fileObject = mock(FileObjectEntity.class);
    when(fileObjectService.get(fileId)).thenReturn(fileObject);
    when(fileObject.getOssUri()).thenReturn("oss-test");
    when(fileObject.getFileName()).thenReturn("report.pdf");
    when(fileObject.getHttpMimeType()).thenReturn("application/pdf");
    try (var userHelper = mockStatic(UserHelper.class);
        var storage = mockStatic(AliCloudObjectStorage.class)) {
      userHelper.when(UserHelper::hasAdminPermission).thenReturn(true);
      storage
          .when(() -> AliCloudObjectStorage.signFileUrl(eq("oss-test"), any(Date.class)))
          .thenReturn(URI.create("https://oss.example.com/report.pdf?sig=x").toURL());
      var access = new FileService(fileObjectService).getFileAccess(fileId);
      assertThat(access.url()).startsWith("https://oss.example.com/");
      assertThat(access.fileName()).isEqualTo("report.pdf");
      assertThat(access.mimeType()).isEqualTo("application/pdf");
      assertThat(access.expiresAt()).isPositive();
    }
  }

  @Test
  public void deniesAccessWhenNeitherOwnerNorAdmin() {
    var fileId = UUID.randomUUID();
    var fileObjectService = mock(FileObjectService.class);
    var fileObject = mock(FileObjectEntity.class);
    when(fileObjectService.get(fileId)).thenReturn(fileObject);
    when(fileObject.getCreatorId()).thenReturn(UUID.randomUUID());
    try (var userHelper = mockStatic(UserHelper.class)) {
      userHelper.when(UserHelper::hasAdminPermission).thenReturn(false);
      userHelper.when(UserHelper::getCurrentUserId).thenReturn(UUID.randomUUID());
      assertThatThrownBy(() -> new FileService(fileObjectService).getFileAccess(fileId))
          .isInstanceOf(PermissionDeniedException.class);
    }
  }
}
