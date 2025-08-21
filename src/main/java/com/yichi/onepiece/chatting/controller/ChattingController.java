package com.yichi.onepiece.chatting.controller;


import com.yichi.onepiece.api.ChattingApi;
import com.yichi.onepiece.chatting.service.ChattingService;
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
        iamService.checkPermissionOfCurrentAccount(
                ResourceType.CHATTING, Set.of(Permission.CREATE, Permission.GET));
        return ResponseEntity.ok(chattingService.create(createChatRequest));
    }

    @Override
    public ResponseEntity<Chat> joinChat(JoinChatRequest joinChatRequest) {
        iamService.checkPermissionOfCurrentAccount(
                ResourceType.CHATTING, Set.of(Permission.UPDATE, Permission.GET));
        return ResponseEntity.ok(chattingService.joinChat(joinChatRequest));
    }

    @Override
    public ResponseEntity<ListChatResponse> listChat(ListChatRequest listChatRequest) {
        iamService.checkPermissionOfCurrentAccount(
                ResourceType.CHATTING, Set.of(Permission.LIST));
        return ResponseEntity.ok(new ListChatResponse().chats(chattingService.list(listChatRequest)));
    }
}
