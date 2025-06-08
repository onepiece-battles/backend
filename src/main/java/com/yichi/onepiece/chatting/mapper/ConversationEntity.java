package com.yichi.onepiece.chatting.mapper;

import com.yichi.infra.support.entity.Entity;

import java.util.UUID;


@Entity
public interface ConversationEntity {
     UUID getId();
     Boolean getGroup();
     String getName();
     Boolean getPrivateGroup();
     Long getCreationRecordId();
     Long getModificationRecordId();
}
