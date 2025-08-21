package com.yichi.onepiece.chatting.controller;

import com.yichi.onepiece.api.MessageApi;
import com.yichi.onepiece.chatting.service.MessageService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.Set;

@Controller
public class MessageController implements MessageApi {

    private final MessageService messageService;
    private final IamService iamService;

    public MessageController(MessageService messageService, IamService iamService) {
        this.messageService = messageService;
        this.iamService = iamService;
    }

    @Override
    public ResponseEntity<JumpToMessageResponse> jumpToMessage(JumpToMessageRequest jumpToMessageRequest) {
        iamService.checkPermissionOfCurrentAccount(
                ResourceType.CHATTING, Set.of(Permission.LIST));
        return ResponseEntity.ok(messageService.jumpToMessage(jumpToMessageRequest));
    }

    @Override
    public ResponseEntity<ListMessageResponse> listMessage(ListMessageRequest listMessageRequest) {
        iamService.checkPermissionOfCurrentAccount(
                ResourceType.CHATTING, Set.of(Permission.LIST));
        return ResponseEntity.ok(messageService.listMessage(listMessageRequest));
    }

    @Override
    public ResponseEntity<RetractMessageResponse> retractMessage(RetractMessageRequest retractMessageRequest) {
        return ResponseEntity.ok(messageService.retractMessage(retractMessageRequest));
    }

    @Override
    public ResponseEntity<SendMessageResponse> sendMessage(SendMessageRequest sendMessageRequest) {
        return ResponseEntity.ok(messageService.sendMessage(sendMessageRequest));
    }
}
