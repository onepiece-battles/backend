package com.yichi.onepiece.quartz.jobs;

import com.yichi.onepiece.email.EmailClient;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import static com.yichi.onepiece.constants.ConstantsContainer.*;

@Component
public class EmailJob extends QuartzJobBean {
    private final Logger logger = LoggerFactory.getLogger(EmailJob.class);
    private final EmailClient emailClient;

    public EmailJob(EmailClient emailClient) {
        this.emailClient = emailClient;
    }

    @Override
    protected void executeInternal(@NonNull JobExecutionContext context) throws JobExecutionException {
        var emailInfoMap =  context.getJobDetail().getJobDataMap();
        String subject = emailInfoMap.getString(SUBJECT);
        String body = emailInfoMap.getString(BODY);
        String receiverEmailAddress = emailInfoMap.getString(RECEIVER_EMAIL_ADDRESS);
        sendEmail(subject, body, receiverEmailAddress);
        try {
            emailClient.sendMail(receiverEmailAddress, body, subject);
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    void sendEmail(String subject, String body, String address) {
        logger.info("sending email {}, {}, {}", subject,body, address);
    }
}
