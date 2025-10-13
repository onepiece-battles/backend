package com.yichi.onepiece.chatting.service;

import com.yichi.onepiece.chatting.model.*;
import com.yichi.onepiece.configuration.ChattingWebSocketHandler;
import com.yichi.onepiece.exception.FileStorageException;
import com.yichi.onepiece.image.service.ImageService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
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
        var messages = messageDao.listMessageFullView(request.getChatId(), request.getLimit());
        return new ListMessageResponse().chats(messages.stream().map(e-> ChatConverter.INSTANCE.toMessage(e)).toList());
    }

    public RetractMessageResponse retractMessage(RetractMessageRequest request) {
        var message = messageDao.findById(request.getMessageId()).orElseThrow();
        message = message.toBuilder().setRetracted(true).build();
        messageDao.update(message);
        return new RetractMessageResponse().result(Boolean.TRUE);
    }

    public SendMessageResponse sendMessage(SendMessageRequest request) {
        logger.info("sending message to db");
        logger.info(request.toString());
        MessageEntityBuilder messageEntityBuilder=MessageEntityBuilder.newBuilder();
        var actionRecord = actionRecordService.createWithUserId(request.getUserId());
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

    public JumpToMessageResponse jumpToMessage(JumpToMessageRequest request) {
        UUID messageId = chattingService.getLastReadMessageId(request.getUserId(), request.getConversationId());
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
}
