package com.yichi.onepiece.file.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;
import javax.annotation.Nullable;

@Entity
public interface FileObjectEntity {
  UUID getId();
  String getFileName();
  @Nullable
  UUID getResourceId();
  UUID getCreatedByUserId();
  Long getCreationRecordId();
  FileCategory getFileType();
  String getHttpMimeType();
  String getOssUri();
}
