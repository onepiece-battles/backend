package com.yichi.onepiece.group.model;

import com.yichi.infra.support.entity.Entity;

import javax.annotation.Nullable;
import java.util.UUID;

@Entity
public interface GroupEntity {
    UUID getId();
    UUID getUserId();
    UUID getRoleId();
    String getName();
    @Nullable
    Byte[] getProfileImage();
    Long getCreationRecordId();
    Long getModificationRecordId();
}
