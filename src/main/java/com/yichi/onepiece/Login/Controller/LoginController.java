package com.yichi.onepiece.Login.Controller;

import com.yichi.onepiece.api.LoginApi;
import com.yichi.onepiece.model.LoginRequest;
import com.yichi.onepiece.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class LoginController implements LoginApi {

  @Override
  public ResponseEntity<User> login(LoginRequest loginRequest) {
    return null;
  }
}
