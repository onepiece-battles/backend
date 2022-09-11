package com.yichi.onepiece.Email;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import static com.yichi.onepiece.Constants.ConstantsContainer.FROM_EMAIL_ADDRESS;

@Component
public class EmailClient {
    private final JavaMailSender javaMailSender;
    private final String from;

    public EmailClient(JavaMailSender mailSender) {
        this.javaMailSender = mailSender;
        this.from = FROM_EMAIL_ADDRESS;
    }

    public void sendMail(String toEmail, String body, String subject) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        // body part to save img
        MimeBodyPart picturePart = new MimeBodyPart();
        DataHandler dataHandler = new DataHandler(new FileDataSource("src/test/resources/email_resources/sunny.png"));
        picturePart.setDataHandler(dataHandler);
        picturePart.setContentID("sunnyPicture");
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("<img src='cid:sunnyPicture' width=100% height=200/>" + body, "UTF-8", "HTML");
        // multipart to combine text and img
        MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(picturePart);
        multipart.setSubType("related");

        mimeMessage.setContent(multipart);

        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
        helper.setFrom(from);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        javaMailSender.send(mimeMessage);
        System.out.println("message sent successfully");
    }
}
