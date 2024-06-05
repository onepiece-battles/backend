package com.yichi.onepiece.utils;

import com.yichi.onepiece.data_export.service.DataExportService;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.OnepieceLocale;
import com.yichi.onepiece.user.UserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class I18nHelper {
    private static final Logger logger = LoggerFactory.getLogger(DataExportService.class);

    public static String getTextByLocal(I18nText text, String locale) {
        return locale.equals(
                OnepieceLocale.ZH_CN.getValue())?text.getZhCN():text.getEnUS();
    }
}
