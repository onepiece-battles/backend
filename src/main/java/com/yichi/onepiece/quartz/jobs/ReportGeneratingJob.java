package com.yichi.onepiece.quartz.jobs;

import com.yichi.onepiece.data_export.service.DataExportService;
import com.yichi.onepiece.model.OnepieceLocale;
import com.yichi.onepiece.user.UserHelper;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import javax.annotation.Nonnull;
import java.util.UUID;

import static com.yichi.onepiece.constants.OnepieceConstants.*;

@Component
public class ReportGeneratingJob extends QuartzJobBean {
    private final Logger logger = LoggerFactory.getLogger(ReportGeneratingJob.class);

    private final DataExportService dataExportService;

    public ReportGeneratingJob(DataExportService dataExportService) {
        this.dataExportService = dataExportService;
    }

    @Override
    protected void executeInternal(@Nonnull JobExecutionContext context) {
        var jobDetailMap = context.getJobDetail().getJobDataMap();
        String battleId = jobDetailMap.getString(BATTLE_ID);
        String userId = jobDetailMap.getString(USER_ID);
        String userEmail = jobDetailMap.getString(USER_EMAIL);
        String locale = jobDetailMap.getString(LOCALE);
        logger.info("locale {}", locale);
        // will be used later
        String userNickname = jobDetailMap.getString(USER_NICKNAME);
        String roleName = jobDetailMap.getString(ROLE_NAME);
        try{
            dataExportService.generateBattleResultPdf(UUID.fromString(battleId), userEmail,
                    locale,
                    UUID.fromString(userId), roleName);
        }catch (Exception e) {
            logger.error("failed to run report generation job for battle {}", battleId, e);
        }
    }
}
