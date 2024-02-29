package com.yichi.onepiece.quartz.jobs;

import com.yichi.onepiece.data_export.service.DataExportService;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import javax.annotation.Nonnull;
import java.util.UUID;

import static com.yichi.onepiece.constants.ConstantsContainer.*;

@Component
public class ReportGeneratingJob extends QuartzJobBean {
    private final Logger logger = LoggerFactory.getLogger(ReportGeneratingJob.class);

    private final DataExportService dataExportService;

    public ReportGeneratingJob(DataExportService dataExportService) {
        this.dataExportService = dataExportService;
    }

    @Override
    protected void executeInternal(@Nonnull JobExecutionContext context) {
        String battleId = context.getJobDetail().getJobDataMap().getString(BATTLE_ID);
        String userId = context.getJobDetail().getJobDataMap().getString(USER_ID);
        String userEmail = context.getJobDetail().getJobDataMap().getString(USER_EMAIL);
        String userNickname = context.getJobDetail().getJobDataMap().getString(USER_NICKNAME);
        try{
            dataExportService.generateBattleResultPdf(UUID.fromString(battleId), userEmail, userNickname, UUID.fromString(userId));
        }catch (Exception e) {
            logger.error(e.toString());
        }
    }
}
