package com.yichi.onepiece.pdf_export.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface FileMetaInfoEntity {
  UUID getId();
  UUID getCreatedByUserId();
  Long getCreationRecordId();
  FileType getFileType();
  FileFormat getFileFormat();
  String getOssPath();
}
