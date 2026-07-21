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

    /// Distinguishes "found an existing conversation for this participant
    /// set" from "created a brand new one" — a retried CREATE_CHAT request
    /// (network retry, duplicate click) must not re-trigger the one-time
    /// "chat created"/"friend request accepted" welcome message.
    public record ChatCreationResult(Chat chat, boolean created) {}

    public Chat create(CreateChatRequest request) {
        return create(request, UserHelper.getCurrentUserId());
    }

    public Chat create(CreateChatRequest request, UUID creatorUserId) {
        return createWithResult(request, creatorUserId).chat();
    }

    public ChatCreationResult createWithResult(CreateChatRequest request, UUID creatorUserId) {
        if (!request.getIds().contains(creatorUserId)) {
            throw new PermissionDeniedException("Access Denied");
        }
        List<User> users  = userService.listByUserIds(request.getIds());
        logger.info("list user ids");
        logger.info(request.getIds().toString());
        List<ConversationEntity> conversationEntities = conversationDao.listByUserIds(request.getIds(), request.getIds().size());
        if (!conversationEntities.isEmpty()) {
            return new ChatCreationResult(ChatConverter.INSTANCE.toChat(conversationEntities.get(0)), false);
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
        Chat chat = ChatConverter.INSTANCE.toChat(
                conversationDao.findChatFullVieById(conversationEntity.getId()).orElseThrow());
        return new ChatCreationResult(chat, true);
    }

    private void createUserConversation(List<UUID> userIds, UUID conversationId) {
        // Idempotent: skip ids that are already members so a retried
        // request (network retry, duplicate click, or a race between two
        // callers) doesn't insert duplicate user_conversation rows, which
        // would make listChat/initializeSession replay the same chat and
        // its message history multiple times.
        var existingUserIds = userConversationDao.listByConversationId(conversationId).stream()
                .map(UserConversationEntity::getUserId)
                .collect(Collectors.toSet());
        List<UserConversationEntity> uc = userIds.stream()
                .filter(id -> !existingUserIds.contains(id))
                .map(e-> UserConversationEntityBuilder.newBuilder().setUserId(e).setConversationId(conversationId).build())
                .toList();
        if (!uc.isEmpty()) {
            userConversationDao.batchInsert(uc);
        }
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
        // The caller must already belong to the conversation to add anyone
        // (including themselves) to it — otherwise anyone who merely knows
        // or guesses a conversationId could force their way into any
        // conversation and add arbitrary other users to it.
        boolean joinerIsMember = userConversationDao.listByConversationId(joinChatRequest.getConversationId())
                .stream()
                .anyMatch(uc -> joinerUserId.equals(uc.getUserId()));
        if (!joinerIsMember) {
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
