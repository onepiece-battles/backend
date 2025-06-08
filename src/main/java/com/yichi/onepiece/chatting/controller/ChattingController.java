package com.yichi.onepiece.chatting.controller;


import com.yichi.onepiece.api.ChattingApi;
import com.yichi.onepiece.chatting.ChattingService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.Set;

@Controller
public class ChattingController implements ChattingApi {
    private final ChattingService chattingService;
    private final IamService iamService;

    public ChattingController(ChattingService chattingService, IamService iamService) {
        this.chattingService = chattingService;
        this.iamService = iamService;
    }

    @Override
    public ResponseEntity<Chat> createChat(CreateChatRequest createChatRequest) {
        // replace in the future
        iamService.checkPermissionOfCurrentAccount(
                ResourceType.BATTLE, Set.of(Permission.CREATE, Permission.GET));
        return ResponseEntity.ok(chattingService.create(createChatRequest));
    }

    @Override
    public ResponseEntity<Chat> joinChat(JoinChatRequest joinChatRequest) {
        // replace in the future
        iamService.checkPermissionOfCurrentAccount(
                ResourceType.BATTLE, Set.of(Permission.CREATE, Permission.GET));
        return ResponseEntity.ok(chattingService.joinChat(joinChatRequest));
    }

    @Override
    public ResponseEntity<RetractMessageResponse> retractMessage(RetractMessageRequest retractMessageRequest) {
        return null;
    }

    @Override
    public ResponseEntity<SendMessageResponse> sendMessage(SendMessageRequest sendMessageRequest) {
        return null;
    }
}
