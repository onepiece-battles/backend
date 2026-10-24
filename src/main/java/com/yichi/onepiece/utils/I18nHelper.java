package com.yichi.onepiece.utils;

import com.yichi.onepiece.data_export.service.DataExportService;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.OnepieceLocale;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.model.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class I18nHelper {
    private static final Logger logger = LoggerFactory.getLogger(DataExportService.class);

    public static String getTextByLocal(I18nText text, String locale) {
        return locale.equals(
                OnepieceLocale.ZH_CN.getValue())?text.getZhCN():text.getEnUS();
    }

    public static I18nText getFriendRequestMessage(UserEntity user) {
        return new I18nText()
                .enUS(String.format("Friend request from sent from %s.", user.getNickname()))
                .zhCN(String.format("来自%s的好友申请。", user.getNickname()));
    }

    public static I18nText getFriendAcceptMessage(UserEntity user) {
        return new I18nText()
                .enUS(String.format("Friend request accepted by %s.", user.getNickname()))
                .zhCN(String.format("%s同意好友申请。", user.getNickname()));
    }

    public static I18nText getFriendRejectMessage(UserEntity user) {
        return new I18nText()
                .enUS(String.format("Friend request rejected by %s.", user.getNickname()))
                .zhCN(String.format("%s拒绝好友申请。", user.getNickname()));
    }

    public static I18nText getSuccess() {
        return new I18nText()
                .enUS("SUCCESS")
                .zhCN("成功");
    }
}
