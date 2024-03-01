package com.yichi.onepiece.email;

import static com.yichi.onepiece.constants.OnepieceConstants.FROM_EMAIL_ADDRESS;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailClient {
  private final Logger logger = LoggerFactory.getLogger(EmailClient.class);
  private final JavaMailSender javaMailSender;

  public EmailClient(JavaMailSender mailSender) {
    this.javaMailSender = mailSender;
  }

  public void sendMail(String receiverEmailAddress, String body, String subject) throws MessagingException {
    MimeMessage mimeMessage = javaMailSender.createMimeMessage();
    // body part to save img
    MimeMultipart multipart = getMimeMultipart(body);
    mimeMessage.setContent(multipart);
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
    helper.setFrom(FROM_EMAIL_ADDRESS);
    helper.setTo(receiverEmailAddress);
    helper.setSubject(subject);
    try{
      javaMailSender.send(mimeMessage);
    } catch (MailException e) {
      logger.error(e.toString());
    }
  }

  private static MimeMultipart getMimeMultipart(String body) throws MessagingException {
    MimeBodyPart picturePart = new MimeBodyPart();
    DataHandler dataHandler =
        new DataHandler(new FileDataSource("src/test/resources/email_resources/sunny.png"));
    picturePart.setDataHandler(dataHandler);
    picturePart.setContentID("sunnyPicture");
    MimeBodyPart textPart = new MimeBodyPart();
    textPart.setText("<img src='cid:sunnyPicture' width=100% height=200/>" + body, "UTF-8", "HTML");
    // multipart to combine text and img
    MimeMultipart multipart = new MimeMultipart();
    multipart.addBodyPart(textPart);
    multipart.addBodyPart(picturePart);
    multipart.setSubType("related");
    return multipart;
  }
}
