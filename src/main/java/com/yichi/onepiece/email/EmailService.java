package com.yichi.onepiece.email;


import com.yichi.onepiece.model.*;
import com.yichi.onepiece.quartz.jobs.EmailJob;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.model.EmailVerificationDao;
import com.yichi.onepiece.user.model.EmailVerificationEntity;
import com.yichi.onepiece.user.model.EmailVerificationEntityBuilder;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.utils.RandomGenerator;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static com.yichi.onepiece.constants.OnepieceConstants.*;
import static com.yichi.onepiece.constants.OnepieceConstants.EMAIL_JOB_GROUP;
import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.*;
import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.VERIFICATION_SUCCESS;

@Service
public class EmailService {
    private final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final SchedulerFactoryBean schedulerFactoryBean;
    private final EmailVerificationDao emailVerificationDao;
    private final Clock clock;

    public EmailService(SchedulerFactoryBean schedulerFactoryBean, EmailVerificationDao emailVerificationDao, Clock clock) {
        this.schedulerFactoryBean = schedulerFactoryBean;
        this.emailVerificationDao = emailVerificationDao;
        this.clock = clock;
    }

    public VerifyEmailAddressResponse verifyEmailAddress(VerifyEmailAddressRequest request) {
        // id from  request is user id
        emailVerificationDao.markEmailAsUsed(request.getId(), request.getEmail());
        long creationTimestamp = clock.millis();
        var verificationCode = RandomGenerator.generateVerificationCode();
        var locale = UserHelper.getCurrentUserDetail().getLocale();
        String subject = "";
        String body = "";
        if(OnepieceLocale.EN_US.equals(locale)) {
            subject = "[Onepiece Battles] Email address verification";
            body = String.format("This is the verification email, The verification code is: %s", verificationCode);
        } else {
            subject = "[Onepiece Battles] 电子邮箱验证";
            body = String.format("这是您的验证邮件，验证码为：%s", verificationCode);
        }
        var text = sendEmail(request.getEmail(), subject, body);

        // expires 5 minutes later
        long expirationTimestamp = creationTimestamp + 300000L;
        var emailVerificationEntity =
                EmailVerificationEntityBuilder.newBuilder()
                        .setEmail(request.getEmail())
                        .setUserId(request.getId())
                        .setEmailVerificationCode(verificationCode)
                        .setCreationTimestamp(clock.millis())
                        .setExpirationTimestamp(expirationTimestamp)
                        .isUsed(false)
                        .build();
        emailVerificationDao.insert(emailVerificationEntity);
        // TODO: 2024/2/29 consider the case when email sent failed.
        //boolean result = text.equals(UserFacingConstants.getConstant(EMAIL_SENT).getDisplayName());
        return new VerifyEmailAddressResponse().emailNotification(text);
    }

    public I18nText sendEmail(
            String email,
            String subject,
            String body) {
        try {
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(RECEIVER_EMAIL_ADDRESS, email);
            jobDataMap.put(SUBJECT, subject);
            jobDataMap.put(BODY, body);

            JobDetail emailSendingJob = JobBuilder.newJob(EmailJob.class)
                    .setJobData(jobDataMap)
                    .withIdentity(UUID.randomUUID().toString(), EMAIL_JOB_GROUP)
                    .withDescription("send email job")
                    .build();
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            Trigger emailJobTrigger = TriggerBuilder.newTrigger()
                    .forJob(emailSendingJob)
                    .startNow().build();
            scheduler.start();
            scheduler.scheduleJob(emailSendingJob, emailJobTrigger);
        } catch (Exception e) {
            logger.error(e.toString());
            return UserFacingConstants.getConstant(EMAIL_SENDING_FAILED).getDisplayName();
        }
        return UserFacingConstants.getConstant(EMAIL_SENT).getDisplayName();
    }

    public PasswordRetrievalResponse sendRetrievalEmail(PasswordRetrievalRequest request) {
        emailVerificationDao.markEmailAsUsedNullUserId(request.getEmail());
        long creationTimestamp = clock.millis();
        var verificationCode = RandomGenerator.generateVerificationCode();
        String subject = "";
        String body = "";
        if(OnepieceLocale.EN_US.equals(request.getLocale())) {
            subject = "[Onepiece Battles] Reset Password";
            body = String.format("This is the verification email, The verification code is: %s", verificationCode);
        } else {
            subject = "[Onepiece Battles] 找回密码";
            body = String.format("这是您的验证邮件，验证码为：%s", verificationCode);
        }
        var text = sendEmail(request.getEmail(), subject, body);
        // expires 5 minutes later
        long expirationTimestamp = creationTimestamp + 300000L;
        var emailVerificationEntity =
                EmailVerificationEntityBuilder.newBuilder()
                        .setEmail(request.getEmail())
                        .setUserId(null)
                        .setEmailVerificationCode(verificationCode)
                        .setCreationTimestamp(clock.millis())
                        .setExpirationTimestamp(expirationTimestamp)
                        .isUsed(false)
                        .build();
        emailVerificationDao.insert(emailVerificationEntity);
        boolean result = text.equals(UserFacingConstants.getConstant(EMAIL_SENT).getDisplayName());
        return new PasswordRetrievalResponse().result(result).notification(text);
    }
}
