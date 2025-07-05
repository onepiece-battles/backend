package com.yichi.onepiece.chatting.service;


import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.chatting.model.*;
import com.yichi.onepiece.exception.FileStorageException;
import com.yichi.onepiece.image.service.ImageService;
import com.yichi.onepiece.lineage.model.ActionRecordConverter;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChattingService {
    Logger logger = LoggerFactory.getLogger(ChattingService.class);

    private final ConversationDao conversationDao;
    private final MessageDao messageDao;
    private final UserConversationDao userConversationDao;
    private final ActionRecordService actionRecordService;
    private final UserService userService;
    private final ImageService imageService;

    public ChattingService(ConversationDao conversationDao, MessageDao messageDao, UserConversationDao userConversationDao, UserService userService, ActionRecordService actionRecordService, ImageService imageService) {
        this.conversationDao = conversationDao;
        this.messageDao = messageDao;
        this.userConversationDao = userConversationDao;
        this.userService = userService;
        this.actionRecordService = actionRecordService;
        this.imageService = imageService;
    }

    public Chat create(CreateChatRequest request) {
        List<User> users  = userService.listByUserIds(request.getIds());
        logger.info("list user ids");
        logger.info(request.getIds().toString());
        List<ConversationEntity> conversationEntities = conversationDao.listByUserIds(request.getIds(), request.getIds().size());
        if (!conversationEntities.isEmpty()) {
            return ChatConverter.INSTANCE.toChat(conversationEntities.get(0));
        }
        ActionRecordEntity actionRecordEntity = actionRecordService.create();
        ActionRecord actionRecord = actionRecordService.convertActionRecord(actionRecordEntity);
        String name = getChatName(users.stream().map(e->e.getNickname()).toList());
        ConversationEntity conversationEntity = ConversationEntityBuilder.newBuilder()
                .setGroup(request.getIds().size()>=3)
                .setPrivateGroup(true)
                .setName(name)
                .setCreationRecordId(actionRecordEntity.getId())
                .setModificationRecordId(actionRecordEntity.getId())
                .build();
        conversationDao.insert(conversationEntity);
        createUserConversation(users.stream().map(e->e.getId()).toList(), conversationEntity.getId());
        return new Chat()
                .name(name)
                .members(users)
                .creationRecord(actionRecord)
                .modificationRecord(actionRecord);
    }

    private void createUserConversation(List<UUID> userIds, UUID conversationId) {
        List<UserConversationEntity> uc =userIds.stream()
                .map(e-> UserConversationEntityBuilder.newBuilder().setUserId(e).setConversationId(conversationId).build())
                .toList();
        userConversationDao.batchInsert(uc);
    }

    private String getChatName(List<String> userNames) {
        return userNames.stream()
                .collect(Collectors.joining(", "));
    }

    public Chat joinChat(JoinChatRequest joinChatRequest) {
        createUserConversation(joinChatRequest.getIds(),joinChatRequest.getConversationId());
        var conversationEntity = conversationDao.findById(joinChatRequest.getConversationId()).orElseThrow();
        var creationRecord = actionRecordService.findOrFail(conversationEntity.getCreationRecordId());
        var modificationRecord = actionRecordService.findOrFail(conversationEntity.getModificationRecordId());
        List<UUID> userIds = userConversationDao.listByConversationId(joinChatRequest.getConversationId())
                .stream().map(e->e.getUserId()).toList();
        return new Chat()
                .creationRecord(ActionRecordConverter.INSTANCE.toActionRecord(creationRecord))
                .modificationRecord(ActionRecordConverter.INSTANCE.toActionRecord(modificationRecord))
                .name(conversationEntity.getName())
                .members(userService.listByUserIds(userIds));
    }

    public SendMessageResponse sendMessage(SendMessageRequest request) {
        MessageEntityBuilder messageEntityBuilder=MessageEntityBuilder.newBuilder();
        var actionRecord = actionRecordService.create();
        try {
            if (request.getProfileImage()!=null) {
                String uri = imageService.uploadImage(request.getProfileImage());
                messageEntityBuilder.setImageContentUri(uri);
            }
            if (request.getVoice()!=null) {
                String uri = imageService.uploadImage(request.getVoice());
                messageEntityBuilder.setVoiceContentUri(uri);
            }
        } catch (FileStorageException e) {
            e.printStackTrace();
            return new SendMessageResponse().result(Boolean.FALSE);
        }
        MessageEntity messageEntity = messageEntityBuilder.setPreMessageId(request.getPreMessageId())
                .setUserId(request.getUserId())
                .setContent(request.getContent())
                .setRetracted(Boolean.FALSE)
                .setConversationId(request.getChatId())
                .setImageContentUri(request.getProfileImage())
                .setCreationRecordId(actionRecord.getId())
                .setModificationRecordId(actionRecord.getId())
                .build();
        messageDao.insert(messageEntity);
        return new SendMessageResponse().result(Boolean.TRUE);
    }

    public RetractMessageResponse retractMessage(RetractMessageRequest request) {
        var message = messageDao.findById(request.getMessageId()).orElseThrow();
        message = message.toBuilder().setRetracted(true).build();
        messageDao.update(message);
        return new RetractMessageResponse().result(Boolean.TRUE);
    }

    public List<Chat> list(ListChatRequest request) {
        return conversationDao.listChat(request.getUserId()).stream()
                .map(e->ChatConverter.INSTANCE.toChat(e)).toList();
    }
}
