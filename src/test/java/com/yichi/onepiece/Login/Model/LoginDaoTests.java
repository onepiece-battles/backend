package com.yichi.onepiece.Login.Model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@SpringBootTest
@TestPropertySource(locations = "classpath:test-h2.properties")
public class LoginDaoTests extends AbstractTestNGSpringContextTests {

  @Autowired LoginDao loginDao;

  @Test
  public void testInsertAndGet() {
    UUID userId = UUID.randomUUID();
    UserEntity user =
        UserEntityBuilder.newBuilder()
            .setId(userId)
            .setUserName("Yichi")
            .setOrganization("Yaocheng")
            .build();
    loginDao.insert(user);

    var result = loginDao.getAllUsers();
    assertThat(result.get(0)).isEqualTo(user);
  }
}
