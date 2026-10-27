package com.yichi.onepiece.assistant.service;

import com.yichi.onepiece.assistant.model.AiSettingDao;
import com.yichi.onepiece.assistant.model.AiSettingEntity;
import com.yichi.onepiece.assistant.model.AiSettingEntityBuilder;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.user.UserHelper;
import org.springframework.stereotype.Service;

/**
 * Reads and writes the single-row {@link AiSettingEntity}. The DeepSeek key is
 * held here server-side and is never returned to clients.
 */
@Service
public class AiSettingService {
    static final String DEFAULT_BASE_URL = "https://api.deepseek.com/chat/completions";
    static final String DEFAULT_MODEL = "deepseek-chat";

    private final AiSettingDao aiSettingDao;
    private final ActionRecordService actionRecordService;

    public AiSettingService(AiSettingDao aiSettingDao, ActionRecordService actionRecordService) {
        this.aiSettingDao = aiSettingDao;
        this.actionRecordService = actionRecordService;
    }

    public AiSettingEntity getOrNull() {
        return aiSettingDao.findLatest().orElse(null);
    }

    public boolean isConfigured() {
        var setting = getOrNull();
        return setting != null && setting.getApiKey() != null && !setting.getApiKey().isBlank();
    }

    public String apiKey() {
        var setting = getOrNull();
        return setting == null ? null : setting.getApiKey();
    }

    public String baseUrl() {
        var setting = getOrNull();
        if (setting == null || setting.getBaseUrl() == null || setting.getBaseUrl().isBlank()) {
            return DEFAULT_BASE_URL;
        }
        return setting.getBaseUrl();
    }

    public String model() {
        var setting = getOrNull();
        if (setting == null || setting.getModel() == null || setting.getModel().isBlank()) {
            return DEFAULT_MODEL;
        }
        return setting.getModel();
    }

    /**
     * Upsert the single settings row. A blank {@code apiKey} keeps the stored key,
     * so the super-admin can change the endpoint or model without re-entering it.
     */
    public void save(String apiKey, String baseUrl, String model) {
        ActionRecordEntity record = actionRecordService.createWithUserId(UserHelper.getCurrentUserId());
        AiSettingEntity existing = getOrNull();
        String normalizedBaseUrl = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl.trim();
        String normalizedModel = (model == null || model.isBlank()) ? DEFAULT_MODEL : model.trim();

        if (existing == null) {
            AiSettingEntity entity = AiSettingEntityBuilder.newBuilder()
                    .setApiKey(apiKey == null ? null : apiKey.trim())
                    .setBaseUrl(normalizedBaseUrl)
                    .setModel(normalizedModel)
                    .setCreationRecordId(record.getId())
                    .setModificationRecordId(record.getId())
                    .build();
            aiSettingDao.insert(entity);
            return;
        }

        String nextKey = (apiKey == null || apiKey.isBlank()) ? existing.getApiKey() : apiKey.trim();
        AiSettingEntity updated = existing.toBuilder()
                .setApiKey(nextKey)
                .setBaseUrl(normalizedBaseUrl)
                .setModel(normalizedModel)
                .setModificationRecordId(record.getId())
                .build();
        aiSettingDao.fullUpdate(updated);
    }
}
