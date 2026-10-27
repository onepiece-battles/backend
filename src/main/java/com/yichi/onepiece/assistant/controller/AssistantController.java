package com.yichi.onepiece.assistant.controller;

import com.yichi.onepiece.assistant.dto.AssistantDto.AiChatRequest;
import com.yichi.onepiece.assistant.dto.AssistantDto.AiChatResponse;
import com.yichi.onepiece.assistant.dto.AssistantDto.AiSettingRequest;
import com.yichi.onepiece.assistant.dto.AssistantDto.AiSettingResponse;
import com.yichi.onepiece.assistant.dto.AssistantDto.ListAiMessagesResponse;
import com.yichi.onepiece.assistant.dto.AssistantDto.SaveAiMessageRequest;
import com.yichi.onepiece.assistant.dto.AssistantDto.SaveAiMessageResponse;
import com.yichi.onepiece.assistant.dto.AssistantDto.SimpleResult;
import com.yichi.onepiece.assistant.service.AiAssistantService;
import com.yichi.onepiece.assistant.service.AiSettingService;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.user.UserHelper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI assistant endpoints. All require a valid JWT (enforced by the security
 * chain). Provider-setting reads/writes additionally require the NIKA
 * super-admin; the DeepSeek key is never returned.
 */
@RestController
@RequestMapping("/api/onepiece/assistant")
public class AssistantController {
    private final AiAssistantService assistantService;
    private final AiSettingService settingService;

    public AssistantController(AiAssistantService assistantService, AiSettingService settingService) {
        this.assistantService = assistantService;
        this.settingService = settingService;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(@RequestBody AiChatRequest request) {
        return assistantService.chat(request.requestBody());
    }

    @PostMapping("/list_messages")
    public ListAiMessagesResponse listMessages() {
        return new ListAiMessagesResponse(assistantService.listMessages(UserHelper.getCurrentUserId()));
    }

    @PostMapping("/save_message")
    public SaveAiMessageResponse saveMessage(@RequestBody SaveAiMessageRequest request) {
        String id = assistantService.saveMessage(
                UserHelper.getCurrentUserId(), request.role(), request.content(), request.resources());
        return new SaveAiMessageResponse(id);
    }

    @PostMapping("/clear_messages")
    public SimpleResult clearMessages() {
        assistantService.clear(UserHelper.getCurrentUserId());
        return new SimpleResult(true);
    }

    @PostMapping("/get_setting")
    public AiSettingResponse getSetting() {
        requireNika();
        return new AiSettingResponse(
                settingService.isConfigured(), settingService.baseUrl(), settingService.model());
    }

    @PostMapping("/set_setting")
    public SimpleResult setSetting(@RequestBody AiSettingRequest request) {
        requireNika();
        settingService.save(request.apiKey(), request.baseUrl(), request.model());
        return new SimpleResult(true);
    }

    private void requireNika() {
        if (!UserHelper.hasNikaPermission()) {
            throw new PermissionDeniedException("Access Denied");
        }
    }
}
