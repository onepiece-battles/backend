package com.yichi.onepiece.chatting.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.model.Chat;
import com.yichi.onepiece.model.Message;
import com.yichi.onepiece.user.model.UserConverter;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {EntityBuilder.class, UserConverter.class})
public interface ChatConverter {
    ChatConverter INSTANCE = Mappers.getMapper(ChatConverter.class);

    Chat toChat(ConversationFullView conversationFullView);
    Chat toChat(ConversationEntity conversationEntity);

    Message toMessage(MessageEntity messageFullView);
}
