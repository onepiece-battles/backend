package com.yichi.onepiece.chatting.service;

import com.yichi.onepiece.chatting.model.*;
import com.yichi.onepiece.exception.FileStorageException;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.image.service.ImageService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.UserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MessageService {
    Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final MessageDao messageDao;
    private final ChattingService chattingService;
    private final ActionRecordService actionRecordService;
    private final ImageService imageService;

    public MessageService(MessageDao messageDao, ChattingService chattingService, ActionRecordService actionRecordService, ImageService imageService) {
        this.messageDao = messageDao;
        this.chattingService = chattingService;
        this.actionRecordService = actionRecordService;
        this.imageService = imageService;
    }

    public ListMessageResponse listMessage(ListMessageRequest request) {
        checkCurrentUserInConversation(request.getChatId());
        var messages = messageDao.listMessageFullView(request.getChatId(), request.getLimit());
        return new ListMessageResponse().chats(messages.stream().map(e-> ChatConverter.INSTANCE.toMessage(e)).toList());
    }

    public RetractMessageResponse retractMessage(RetractMessageRequest request) {
        var message = messageDao.findById(request.getMessageId()).orElseThrow();
        if (!UserHelper.getCurrentUserId().equals(message.getUserId())) {
            throw new PermissionDeniedException("Access Denied");
        }
        message = message.toBuilder().setRetracted(true).build();
        messageDao.update(message);
        return new RetractMessageResponse().result(Boolean.TRUE);
    }

    public SendMessageResponse sendMessage(SendMessageRequest request) {
        return sendMessage(request, UserHelper.getCurrentUserId(), true);
    }

    public SendMessageResponse sendMessage(SendMessageRequest request, UUID senderUserId) {
        return sendMessage(request, senderUserId, false);
    }

    private SendMessageResponse sendMessage(SendMessageRequest request, UUID senderUserId, boolean checkMembership) {
        if (checkMembership) {
            checkCurrentUserInConversation(request.getConversationId());
        }
        MessageEntityBuilder messageEntityBuilder=MessageEntityBuilder.newBuilder();
        var actionRecord = actionRecordService.createWithUserId(senderUserId);
        String imageUri = "";
        String voiceUri = "";
        try {
            if (request.getProfileImage() != null && !request.getProfileImage().isBlank()) {
                logger.info(
                        "uploading image message for conversation {}, encoded image length {}",
                        request.getConversationId(),
                        request.getProfileImage().length());
                imageUri = imageService.uploadImage(request.getProfileImage());
                messageEntityBuilder.setImageContentUri(imageUri);
            }
            if (request.getVoice() != null && !request.getVoice().isBlank()) {
                voiceUri = imageService.uploadImage(request.getVoice());
                messageEntityBuilder.setVoiceContentUri(voiceUri);
            }
        } catch (Exception e) {
            logger.error("failed to upload message attachment for conversation {}", request.getConversationId(), e);
            return new SendMessageResponse().result(Boolean.FALSE);
        }
        MessageEntity messageEntity = messageEntityBuilder.setPreMessageId(request.getPreMessageId())
                .setUserId(senderUserId)
                .setContent(request.getContent())
                .setRetracted(Boolean.FALSE)
                .setImageContentUri(imageUri.isEmpty()?null: imageUri)
                .setVoiceContentUri(voiceUri.isEmpty()?null: voiceUri)
                .setConversationId(request.getConversationId())
                .setCreationRecordId(actionRecord.getId())
                .setModificationRecordId(actionRecord.getId())
                .build();
        messageDao.insert(messageEntity);
        var message = messageDao.getMessageFullView(messageEntity.getId()).orElseThrow();
        return new SendMessageResponse().result(Boolean.TRUE).message(ChatConverter.INSTANCE.toMessage(message));
    }

    public JumpToMessageResponse jumpToMessage(JumpToMessageRequest request) {
        UUID currentUserId = UserHelper.getCurrentUserId();
        checkCurrentUserInConversation(request.getConversationId());
        UUID messageId = chattingService.getLastReadMessageId(currentUserId, request.getConversationId());
        if(messageId==null) {
            return new JumpToMessageResponse();
        }
        MessageEntity messageEntity = messageDao.findById(messageId).orElseThrow();
        List<MessageFullView> messageFullViewList = messageDao.listMessageFullViewByLastMessageId(request.getConversationId(),messageEntity.getCreationRecordId());
        return new JumpToMessageResponse().messages(messageFullViewList.stream().map(e-> ChatConverter.INSTANCE.toMessage(e)).toList());
    }

    public List<MessageFullView> getRecentMessage(UUID conversationId) {
        return messageDao.listMessageFullView(conversationId, 20);
    }

    private void checkCurrentUserInConversation(UUID conversationId) {
        UUID currentUserId = UserHelper.getCurrentUserId();
        var isMember =
                chattingService.listUserConversationByUserId(currentUserId).stream()
                        .anyMatch(conversation -> conversationId.equals(conversation.getConversationId()));
        if (!isMember) {
            throw new PermissionDeniedException("Access Denied");
        }
    }
}
