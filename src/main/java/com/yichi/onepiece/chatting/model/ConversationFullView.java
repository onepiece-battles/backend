package com.yichi.onepiece.chatting.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.user.model.UserEntity;

import java.util.List;

@Entity
public interface ConversationFullView extends ConversationEntity{
    List<UserEntity> getMembers();
    ActionRecordEntity getModificationRecord();
    ActionRecordEntity getCreationRecord();
}
