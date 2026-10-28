package com.yichi.onepiece.support.service;


import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.image.service.ImageService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import com.yichi.onepiece.utils.EncryptHelper;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import javax.annotation.Nullable;
import java.io.*;
import java.util.UUID;

@Service
public class SupportService {
    private final UserService userService;
    private final CharacterService characterService;
    private final ImageService imageService;
    private final GroupService groupService;
    private final FruitService fruitService;

    public SupportService(UserService userService, CharacterService characterService, ImageService imageService, GroupService groupService, FruitService fruitService) {
        this.userService = userService;
        this.characterService = characterService;
        this.imageService = imageService;
        this.groupService = groupService;
        this.fruitService = fruitService;
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
            case FRUIT -> {
                var fruits = fruitService.listNoUriFruits();
                for (var fruit: fruits) {
                    imageService.uploadImage(fruit.getId(),
                            ResourceType.FRUIT, EncryptHelper.base64Encode(ArrayUtils.toPrimitive(fruit.getProfileImage())));
                }
            }
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

    public BatchCreateFruitResponse batchCreateFruit(BatchCreateFruitRequest request) {

        String csvFile = "fruit.csv"; // Change to your file path
        ClassPathResource classPathResource = new ClassPathResource(csvFile);
        try (InputStreamReader inputStreamReader = new InputStreamReader(classPathResource.getInputStream());
             CSVParser csvParser = new CSVParser(inputStreamReader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
                    for (CSVRecord record : csvParser) {
                CreateFruitRequest createFruitRequest = new CreateFruitRequest();
                createFruitRequest.name(new I18nText().enUS(record.get(0).trim()).zhCN(record.get(1).trim()))
                        .userId(UserHelper.getCurrentUserId())
                        .fruitType(FruitType.valueOf(record.get(2).trim()))
                        .energyConsume(Integer.valueOf(record.get(3).trim()))
                        .attackSlope(Double.valueOf(record.get(4).trim()))
                        .defenseSlope(Double.valueOf(record.get(5).trim()))
                        .intelligenceSlope(Double.valueOf(record.get(6).trim()))
                        .groupGainSlope(Double.valueOf(record.get(7).trim()))
                        .bloodSlope(Double.valueOf(record.get(8).trim()))
                        .attackIntercept(Integer.valueOf(record.get(9).trim()))
                        .defenseIntercept(Integer.valueOf(record.get(10).trim()))
                        .speedIntercept(Integer.valueOf(record.get(11).trim()))
                        .intelligenceIntercept(Integer.valueOf(record.get(12).trim()))
                        .groupGainIntercept(Integer.valueOf(record.get(13).trim()))
                        .bloodIntercept(Integer.valueOf(record.get(14).trim()))
                        .speedSlope(Double.valueOf(record.get(15).trim()));
                fruitService.createFruit(createFruitRequest);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new BatchCreateFruitResponse().result(UserFacingConstants.getConstant(UserFacingConstantsKey.FRUIT_BATCH_CREATE_SUCCESS)
                .getDisplayName());
    }


    public void updateCharacterAgeHeightBounty() {
        characterService.updateBountyAgeAndHeight();
    }
}
