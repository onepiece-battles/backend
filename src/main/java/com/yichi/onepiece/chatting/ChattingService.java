package com.yichi.onepiece.chatting;


import com.yichi.onepiece.chatting.mapper.*;
import com.yichi.onepiece.lineage.model.ActionRecordConverter;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.service.UserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChattingService {
    private final ConversationDao conversationDao;
    private final PhraseDao phraseDao;
    private final UserConversationDao userConversationDao;
    private final ActionRecordService actionRecordService;
    private final UserService userService;

    public ChattingService(ConversationDao conversationDao, PhraseDao phraseDao, UserConversationDao userConversationDao, UserService userService, ActionRecordService actionRecordService) {
        this.conversationDao = conversationDao;
        this.phraseDao = phraseDao;
        this.userConversationDao = userConversationDao;
        this.userService = userService;
        this.actionRecordService = actionRecordService;
    }

    public Chat create(CreateChatRequest request) {
        List<User> users  = userService.listByUserIds(request.getIds());
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

}
