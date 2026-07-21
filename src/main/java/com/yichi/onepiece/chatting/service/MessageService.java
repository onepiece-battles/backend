package com.yichi.onepiece.chatting.service;

import com.yichi.onepiece.chatting.model.*;
import com.yichi.onepiece.configuration.ChattingWebSocketHandler;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.image.service.ImageService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.UserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MessageService {
    private static final int DEFAULT_MESSAGE_PAGE_SIZE = 20;
    private static final int MAX_MESSAGE_PAGE_SIZE = 100;
    Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final MessageDao messageDao;
    private final ChattingService chattingService;
    private final ActionRecordService actionRecordService;
    private final ImageService imageService;
    // @Lazy breaks the constructor cycle: ChattingWebSocketHandler already
    // depends on MessageService to replay history, and this dependency runs
    // the other way so retraction can broadcast a live update to connected
    // clients instead of only updating the database.
    private final ChattingWebSocketHandler chattingWebSocketHandler;

    public MessageService(MessageDao messageDao, ChattingService chattingService, ActionRecordService actionRecordService, ImageService imageService, @Lazy ChattingWebSocketHandler chattingWebSocketHandler) {
        this.messageDao = messageDao;
        this.chattingService = chattingService;
        this.actionRecordService = actionRecordService;
        this.imageService = imageService;
        this.chattingWebSocketHandler = chattingWebSocketHandler;
    }

    public ListMessageResponse listMessage(ListMessageRequest request) {
        checkCurrentUserInConversation(request.getChatId());
        int limit = boundedPageSize(request.getLimit());
        var messages = messageDao.listMessageFullView(request.getChatId(), limit);
        return new ListMessageResponse().chats(messages.stream().map(e-> ChatConverter.INSTANCE.toMessage(e)).toList());
    }

    public RetractMessageResponse retractMessage(RetractMessageRequest request) {
        var message = messageDao.findById(request.getMessageId()).orElseThrow();
        if (!UserHelper.getCurrentUserId().equals(message.getUserId())) {
            throw new PermissionDeniedException("Access Denied");
        }
        // Retraction must actually remove the content, not just flag it —
        // otherwise every history read (list_message, jump_to_message, the
        // WS reconnect replay) still returns the original text/media with
        // only a `retracted` flag alongside it, leaving enforcement to
        // client-side convention.
        message = message.toBuilder()
                .setRetracted(true)
                .setContent(null)
                .setImageContentUri(null)
                .setVoiceContentUri(null)
                .build();
        messageDao.update(message);

        // Push the retraction to other connected participants immediately —
        // without this, their sessions keep showing the message as live
        // until they happen to refetch history.
        var fullView = messageDao.getMessageFullView(message.getId()).orElseThrow();
        chattingWebSocketHandler.broadcastMessageUpdate(
                message.getConversationId(), ChatConverter.INSTANCE.toMessage(fullView));

        return new RetractMessageResponse().result(Boolean.TRUE);
    }

    public SendMessageResponse sendMessage(SendMessageRequest request) {
        return sendMessage(request, UserHelper.getCurrentUserId(), true);
    }

    public SendMessageResponse sendMessage(SendMessageRequest request, UUID senderUserId) {
        return sendMessage(request, senderUserId, true);
    }

    private SendMessageResponse sendMessage(SendMessageRequest request, UUID senderUserId, boolean checkMembership) {
        if (checkMembership) {
            checkUserInConversation(senderUserId, request.getConversationId());
        }
        MessageEntityBuilder messageEntityBuilder=MessageEntityBuilder.newBuilder();
        String imageUri = "";
        String voiceUri = "";
        try {
            if (request.getProfileImage() != null && !request.getProfileImage().isBlank()) {
                logger.info(
                        "uploading image message for conversation {}, encoded image length {}",
                        request.getConversationId(),
                        request.getProfileImage().length());
                imageUri = imageService.uploadImage(request.getProfileImage());
                request.setProfileImage(null);
                messageEntityBuilder.setImageContentUri(imageUri);
            }
            if (request.getVoice() != null && !request.getVoice().isBlank()) {
                voiceUri = imageService.uploadImage(request.getVoice());
                request.setVoice(null);
                messageEntityBuilder.setVoiceContentUri(voiceUri);
            }
        } catch (Exception e) {
            logger.error("failed to upload message attachment for conversation {}", request.getConversationId(), e);
            return new SendMessageResponse().result(Boolean.FALSE);
        }
        var actionRecord = actionRecordService.createWithUserId(senderUserId);
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
        List<MessageFullView> messageFullViewList =
                messageDao.listMessageFullViewByLastMessageId(
                        request.getConversationId(),
                        messageEntity.getCreationRecordId(),
                        MAX_MESSAGE_PAGE_SIZE);
        return new JumpToMessageResponse().messages(messageFullViewList.stream().map(e-> ChatConverter.INSTANCE.toMessage(e)).toList());
    }

    public List<MessageFullView> getRecentMessage(UUID conversationId) {
        return messageDao.listMessageFullView(conversationId, DEFAULT_MESSAGE_PAGE_SIZE);
    }

    private void checkCurrentUserInConversation(UUID conversationId) {
        checkUserInConversation(UserHelper.getCurrentUserId(), conversationId);
    }

    private void checkUserInConversation(UUID userId, UUID conversationId) {
        var isMember =
                chattingService.listUserConversationByUserId(userId).stream()
                        .anyMatch(conversation -> conversationId.equals(conversation.getConversationId()));
        if (!isMember) {
            throw new PermissionDeniedException("Access Denied");
        }
    }

    private int boundedPageSize(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_MESSAGE_PAGE_SIZE;
        }
        return Math.min(requestedLimit, MAX_MESSAGE_PAGE_SIZE);
    }
}
