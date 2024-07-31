package com.yichi.onepiece.support.controller;
import com.yichi.onepiece.api.SupportApi;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.model.BatchCreateFruitRequest;
import com.yichi.onepiece.model.BatchCreateFruitResponse;
import com.yichi.onepiece.model.UpdateUriRequest;
import com.yichi.onepiece.model.UpdateUriResponse;
import com.yichi.onepiece.support.service.SupportService;
import com.yichi.onepiece.user.UserHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class SupportController implements SupportApi {

    private final SupportService supportService;

    public SupportController(SupportService service) {
        this.supportService = service;
    }

    @Override
    public ResponseEntity<BatchCreateFruitResponse> batchCreateFruit(BatchCreateFruitRequest batchCreateFruitRequest) {
        if (!UserHelper.hasAdminPermission()) {
            throw new PermissionDeniedException("Access Denied");
        }
        var response = supportService.batchCreateFruit(batchCreateFruitRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> updateCharacterBasic() {
        if (!UserHelper.hasAdminPermission()) {
            throw new PermissionDeniedException("Access Denied");
        }
        supportService.updateCharacterAgeHeightBounty();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<UpdateUriResponse> updateUri(UpdateUriRequest updateUriRequest) {
        if (!UserHelper.hasAdminPermission()) {
            throw new PermissionDeniedException("Access Denied");
        }
        UpdateUriResponse response  = supportService.updateUri(
                updateUriRequest.getResourceType(), updateUriRequest.getResourceId());
        return ResponseEntity.ok(response);
    }
}
