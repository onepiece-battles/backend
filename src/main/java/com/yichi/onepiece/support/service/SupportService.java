package com.yichi.onepiece.support.service;


import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.image.service.ImageService;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.model.UpdateUriResponse;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import com.yichi.onepiece.utils.EncryptHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import javax.annotation.Nullable;
import java.util.UUID;

@Service
public class SupportService {
    private final UserService userService;
    private final CharacterService characterService;
    private final ImageService imageService;
    private final GroupService groupService;

    public SupportService(UserService userService, CharacterService characterService, ImageService imageService, GroupService groupService) {
        this.userService = userService;
        this.characterService = characterService;
        this.imageService = imageService;
        this.groupService = groupService;
    }

    public UpdateUriResponse updateUri(ResourceType resourceType, @Nullable UUID resourceId) {
        switch (resourceType) {
            case USER -> {
                var users = userService.listNoUriUsers();
                for (var user: users) {
                    imageService.uploadImage(user.getId(),
                            ResourceType.USER, EncryptHelper.base64Encode(ArrayUtils.toPrimitive(user.getProfileImage())));
                }
            }
            case GROUP -> {
                var groups = groupService.listNoUriGroups();
                for (var group: groups) {
                    imageService.uploadImage(group.getId(),
                            ResourceType.GROUP, EncryptHelper.base64Encode(ArrayUtils.toPrimitive(group.getProfileImage())));
                }
            }
            case FRUIT -> {}
            case CHARACTER -> {
                var characters = characterService.listNoUriCharacters();
                for (var character: characters) {
                    imageService.uploadImage(character.getId(),
                            ResourceType.CHARACTER, EncryptHelper.base64Encode(ArrayUtils.toPrimitive(character.getProfileImage())));
                }
            }
        }
        return new UpdateUriResponse().result(UserFacingConstants.getConstant(UserFacingConstantsKey.IMAGE_UPLOAD_SUCCESS)
                .getDisplayName());
    }
}
