package com.yichi.onepiece.email;

import com.yichi.onepiece.api.EmailApi;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.Set;

@Controller
public class EmailController implements EmailApi {
    private final EmailService emailService;
    private final IamService iamService;

    public EmailController(EmailService emailService, IamService iamService) {
        this.emailService = emailService;
        this.iamService = iamService;
    }

    @Override
    public ResponseEntity<PasswordRetrievalResponse> sendRetrievalEmail(PasswordRetrievalRequest passwordRetrievalRequest) {
        var response =  emailService.sendRetrievalEmail(passwordRetrievalRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<VerifyEmailAddressResponse> verifyEmailAddress(VerifyEmailAddressRequest verifyEmailAddressRequest) {
        iamService.checkPermissionOfCurrentAccount(ResourceType.USER, Set.of(Permission.UPDATE));
        var response = emailService.verifyEmailAddress(verifyEmailAddressRequest);
        return ResponseEntity.ok(response);
    }
}
