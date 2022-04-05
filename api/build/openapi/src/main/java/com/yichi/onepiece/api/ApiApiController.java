package com.yichi.onepiece.api;

import java.util.Optional;
import javax.annotation.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;

@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2022-04-05T23:39:08.017521+08:00[Asia/Shanghai]")
@Controller
@RequestMapping("${openapi.onePieceAPIs.base-path:}")
public class ApiApiController implements ApiApi {

  private final NativeWebRequest request;

  @Autowired
  public ApiApiController(NativeWebRequest request) {
    this.request = request;
  }

  @Override
  public Optional<NativeWebRequest> getRequest() {
    return Optional.ofNullable(request);
  }
}
