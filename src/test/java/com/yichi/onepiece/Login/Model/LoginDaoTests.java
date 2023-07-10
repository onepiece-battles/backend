package com.yichi.onepiece.Login.Model;

import static org.assertj.core.api.Assertions.assertThat;

import com.yichi.onepiece.user.Model.UserAccountCondition;
import com.yichi.onepiece.user.Model.UserDao;
import com.yichi.onepiece.user.Model.UserEntity;
import com.yichi.onepiece.user.Model.UserEntityBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@SpringBootTest
@TestPropertySource(locations = "classpath:test-h2.properties")
public class LoginDaoTests extends AbstractTestNGSpringContextTests {

  @Autowired UserDao loginDao;

  @Test(enabled = false)
  public void testInsertAndGet() {
    UserEntity user =
        UserEntityBuilder.newBuilder()
            .setEncryptedPassword("abc")
            .setAccountCondition(UserAccountCondition.SignIn)
            .setName("yichi")
            .setOrganization("tesla")
            .build();
    loginDao.insert(user);

    var result = loginDao.getAllUsers();
    System.out.println(result);
    assertThat(result.get(0)).isEqualTo(user);
  }
}
