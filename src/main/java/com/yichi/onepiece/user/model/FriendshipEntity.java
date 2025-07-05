package com.yichi.onepiece.user.model;
import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface FriendshipEntity {
    UUID getId();
    UUID getFriendLeftId();
    UUID getFriendRightId();
    FriendshipStatus getFriendshipStatus();
    Long getCreationRecordId();
    Long getModificationRecordId();
    FriendshipEntityBuilder toBuilder();
}
