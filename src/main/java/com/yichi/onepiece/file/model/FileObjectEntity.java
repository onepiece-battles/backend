package com.yichi.onepiece.file.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;
import javax.annotation.Nullable;

@Entity
public interface FileObjectEntity {
  UUID getId();
  String getFileName();
  // if not null, resource id is id of the onepiece-battles resources: [character, battle, fruit, group]
  @Nullable
  UUID getResourceId();
  UUID getCreatorId();
  Long getCreationRecordId();
  FileCategory getFileCategory();
  String getHttpMimeType();
  String getOssUri();
}
