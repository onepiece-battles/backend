package com.yichi.onepiece.Login.Service;

import com.yichi.onepiece.email.EmailClient;
import jakarta.mail.MessagingException;
import org.mockito.testng.MockitoTestNGListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@SpringBootTest
@Listeners(MockitoTestNGListener.class)
public class MailServiceTests extends AbstractTestNGSpringContextTests {
  @Autowired private EmailClient emailService;

  @Test
  public void testSendEmail() throws MessagingException {
    String subject = "Test Email From OnePiece Battles";
    String toEmail = "zhangyichiwind@163.com";
    String body = "<h1>Welcome to OnePiece Battles!</h1>";
    emailService.sendMail(toEmail, body, subject);
  }
}
