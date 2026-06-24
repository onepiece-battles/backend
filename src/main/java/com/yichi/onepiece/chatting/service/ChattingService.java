package com.yichi.onepiece.chatting.service;


import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.chatting.model.*;
import com.yichi.onepiece.exception.FileStorageException;
import com.yichi.onepiece.image.service.ImageService;
import com.yichi.onepiece.lineage.model.ActionRecordConverter;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
        return create(request, UserHelper.getCurrentUserId());
    }

    public Chat create(CreateChatRequest request, UUID creatorUserId) {
        if (!request.getIds().contains(creatorUserId)) {
            throw new PermissionDeniedException("Access Denied");
        }
        List<User> users  = userService.listByUserIds(request.getIds());
        logger.info("list user ids");
        logger.info(request.getIds().toString());
        List<ConversationEntity> conversationEntities = conversationDao.listByUserIds(request.getIds(), request.getIds().size());
        if (!conversationEntities.isEmpty()) {
            return ChatConverter.INSTANCE.toChat(conversationEntities.get(0));
        }
        ActionRecordEntity actionRecordEntity = actionRecordService.createWithUserId(creatorUserId);
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
        return ChatConverter.INSTANCE.toChat(
                conversationDao.findChatFullVieById(conversationEntity.getId()).orElseThrow());
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
        return joinChat(joinChatRequest, UserHelper.getCurrentUserId());
    }

    public Chat joinChat(JoinChatRequest joinChatRequest, UUID joinerUserId) {
        if (!joinChatRequest.getIds().contains(joinerUserId)) {
            throw new PermissionDeniedException("Access Denied");
        }
        createUserConversation(joinChatRequest.getIds(),joinChatRequest.getConversationId());
        var conversationEntity = conversationDao.findById(joinChatRequest.getConversationId()).orElseThrow();
        return  ChatConverter.INSTANCE.toChat(conversationDao.findChatFullVieById(conversationEntity.getId()).orElseThrow());
    }



    public List<Chat> list(ListChatRequest request) {
        return list(UserHelper.getCurrentUserId());
    }

    public List<Chat> list(UUID userId) {
        return conversationDao.listChat(userId).stream()
                .map(e->ChatConverter.INSTANCE.toChat(e)).toList();
    }

    public UUID getLastReadMessageId(UUID userId, UUID conversationId) {
        return userConversationDao.findLastReadMessageId(userId, conversationId);
    }

    public List<UserConversationEntity> listUserConversationByUserId(UUID userId) {
        return userConversationDao.listByUserId(userId);
    }
}
