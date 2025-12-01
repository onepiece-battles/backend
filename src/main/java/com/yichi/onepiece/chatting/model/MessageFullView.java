package com.yichi.onepiece.chatting.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;

@Entity
public interface MessageFullView extends MessageEntity {
    MessageEntity getPreMessage();
    ActionRecordEntity getCreationRecord();
    ActionRecordEntity getModificationRecord();
}
