package com.yichi.onepiece.assistant.agent;

import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.LanguageCode;
import com.yichi.onepiece.model.OnepieceLocale;
import java.util.Locale;

/** The language the assistant answers in and resolves resource names to. */
public enum AssistantLanguage {
    ZH_CN(LanguageCode.ZH_CN),
    EN_US(LanguageCode.EN_US);

    private final LanguageCode languageCode;

    AssistantLanguage(LanguageCode languageCode) {
        this.languageCode = languageCode;
    }

    /**
     * The client's current UI language wins (it can differ from the saved
     * account locale); otherwise the account locale, then Chinese.
     */
    public static AssistantLanguage resolve(String requested, OnepieceLocale accountLocale) {
        if (requested != null) {
            String normalized = requested.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("en")) {
                return EN_US;
            }
            if (normalized.startsWith("zh")) {
                return ZH_CN;
            }
        }
        return accountLocale == OnepieceLocale.EN_US ? EN_US : ZH_CN;
    }

    public LanguageCode languageCode() {
        return languageCode;
    }

    public boolean isChinese() {
        return this == ZH_CN;
    }

    /** Same rule as the client: the preferred language, else the other one. */
    public String displayName(I18nText text) {
        if (text == null) {
            return "";
        }
        String primary = isChinese() ? text.getZhCN() : text.getEnUS();
        String fallback = isChinese() ? text.getEnUS() : text.getZhCN();
        if (primary == null || primary.isBlank()) {
            return fallback == null ? "" : fallback;
        }
        return primary;
    }

    /** Picks the Chinese or English variant of a user-facing string. */
    public String pick(String chinese, String english) {
        return isChinese() ? chinese : english;
    }
}
