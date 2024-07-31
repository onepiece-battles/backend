package com.yichi.onepiece.character.service;

import com.yichi.onepiece.character.helper.CharacterHelper;
import com.yichi.onepiece.character.model.converter.CharacterEntityConverter;
import com.yichi.onepiece.character.model.dao.CharacterDao;
import com.yichi.onepiece.character.model.dao.CharacterToFruitDao;
import com.yichi.onepiece.character.model.dao.CharacterToGroupDao;
import com.yichi.onepiece.character.model.dao.HakiDao;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.character.model.entity.CharacterToFruitEntityBuilder;
import com.yichi.onepiece.character.model.entity.CharacterToGroupEntity;
import com.yichi.onepiece.character.model.entity.CharacterToGroupEntityBuilder;
import com.yichi.onepiece.character.model.entity.HakiEntityBuilder;
import com.yichi.onepiece.constants.OnepieceConstants;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.model.Character;
import com.yichi.onepiece.model.CharacterFullView;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import com.yichi.onepiece.utils.EncryptHelper;

import java.util.*;

import com.yichi.onepiece.utils.RandomGenerator;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CharacterService {
    Logger logger = LoggerFactory.getLogger(CharacterService.class);
    private static final ResourceType RESOURCE_TYPE = ResourceType.CHARACTER;

    private final CharacterDao characterDao;
    private final CharacterToGroupDao characterToGroupDao;
    private final HakiDao hakiDao;
    private final FruitService fruitService;
    private final CharacterHelper characterHelper;

    private final CharacterToFruitDao characterToFruitDao;
    private final ActionRecordService actionRecordService;
    private final UserService userService;

    public CharacterService(
            CharacterDao CharacterDao,
            CharacterToGroupDao characterToGroupDao, HakiDao hakiDao,
            FruitService fruitService, CharacterHelper characterHelper,
            CharacterToFruitDao characterToFruitDao,
            ActionRecordService actionRecordService,
            UserService userService) {
        this.characterDao = CharacterDao;
        this.characterToGroupDao = characterToGroupDao;
        this.hakiDao = hakiDao;
        this.fruitService = fruitService;
        this.characterHelper = characterHelper;
        this.characterToFruitDao = characterToFruitDao;
        this.actionRecordService = actionRecordService;
        this.userService = userService;
    }

    public Character createCharacter(CreateCharacterRequest request) {
        var characterEntity = CharacterEntityConverter.INSTANCE.toCharacterEntity(request);
        var actionRecord = actionRecordService.create();
        // character does not eat any fruit by default.
        characterEntity =
                characterEntity.toBuilder()
                        .setCreationRecordId(actionRecord.getId())
                        .setModificationRecordId(actionRecord.getId())
                        .setSystemResource(UserHelper.hasNikaPermission())
                        .setScore(RandomGenerator.calculateInitialScore(characterEntity))
                        .build();
        characterDao.insert(characterEntity);
        createHakis(request.getHakis(), characterEntity.getId());
        var insertedEntity =
                characterDao
                        .findById(characterEntity.getId())
                        .orElseThrow(
                                ExceptionUtils.notFound(
                                        "Can not find inserted character with id %s", characterEntity.getId()));
        return CharacterEntityConverter.INSTANCE.toCharacter(insertedEntity);
    }

    private void createHakis(List<Haki> hakis, UUID characterId) {
        if (hakis.isEmpty()) {
            return;
        }
        var hakiEntities =
                hakis.stream()
                        .map(
                                haki ->
                                        HakiEntityBuilder.newBuilder()
                                                .setHaki(haki)
                                                .setCharacterId(characterId)
                                                .build())
                        .toList();
        hakiDao.batchInsert(hakiEntities);
    }

    private void updateHaki(UUID characterId, List<Haki> hakis) {
        hakiDao.deleteByCharacterId(characterId);
        createHakis(hakis, characterId);
    }

    public void updateProfile(UUID characterId, String uri) {
        var modificationRecord = actionRecordService.create();
        var character =
                characterDao
                        .findById(characterId)
                        .orElseThrow(ExceptionUtils.notFound("character with id %s not found", characterId));
        var updatedCharacter =
                character.toBuilder()
                        .setModificationRecordId(modificationRecord.getId())
                        .setImageUri(uri)
                        .build();
        characterDao.update(updatedCharacter);
    }

    public CharacterFullView getCharacterFullView(UUID characterId) {
        var characterFullView = characterDao.getFullViewById(characterId).orElseThrow();
        // add next and previous id
        characterFullView = characterFullView.toBuilder()
                .setPreviousCharacter(getPreviousCharacter(characterFullView.getCreationRecord().getId()))
                .setNextCharacter(getNextCharacter(characterFullView.getCreationRecord().getId()))
                .build();
        return CharacterEntityConverter.INSTANCE.toCharacterFullView(characterFullView);
    }

    /**
     * This function execute the request for a character to join a group.
     * <li> First, mark all groups related to this character not in unless the group id from request matches </li>
     * <li> Insert new group relation if new group not found. </li>
     *
     * @param request the request for a character to join a group.
     * @return Whether this character successfully joined this group.
     */
    public JoinGroupResponse joinGroup(JoinGroupRequest request) {
        var characterToGroups = characterToGroupDao.findByCharacterId(request.getCharacterId());
        var currentGroup = characterToGroups.stream()
                .filter(characterToGroupEntity -> !characterToGroupEntity.getOnceBelongedTo()).findFirst();
        // exit current group if present
        currentGroup.ifPresent(characterToGroupEntity -> characterToGroupDao.update(
                characterToGroupEntity.toBuilder().setOnceBelongedTo(true).build()));
        // update original groups
        var groupMatchEntity = characterToGroups
                .stream()
                .filter(characterToGroupEntity ->
                        characterToGroupEntity.getGroupId().equals(request.getGroupId())
                ).findFirst();
        if (groupMatchEntity.isPresent()) {
            var updatedEntity = groupMatchEntity.get().toBuilder().setOnceBelongedTo(false).build();
            characterToGroupDao.update(updatedEntity);
        } else {
            CharacterToGroupEntity characterToGroupEntity = CharacterToGroupEntityBuilder.newBuilder()
                    .setCharacterId(request.getCharacterId())
                    .setGroupId(request.getGroupId())
                    .position(request.getPosition())
                    .onceBelongedTo(false)
                    .build();
            characterToGroupDao.insert(characterToGroupEntity);
        }
        return new JoinGroupResponse()
                .result(UserFacingConstants.getConstant(UserFacingConstantsKey.GROUP_JOIN_SUCCESS)
                        .getDisplayName());
    }

    public EatFruitResponse eatFruit(UUID characterId, UUID fruitId) {
        var characterEntityOptional = characterDao.findById(characterId);
        if (characterEntityOptional.isEmpty()) {
            throw new RuntimeException("can not add fruit to character that doesn't exist");
        }
        var character = characterEntityOptional.get();
        int fruitLimit = character.getMaximumFruitCount();
        var fruits = fruitService.listFruitEntityByCharacterId(characterId);
        EatFruitResponse response = new EatFruitResponse();
        if (fruits.size() == fruitLimit) {
            // fruit count exceeds the maximum permitted count.
            return response.fruitMessage(
                    UserFacingConstants.getConstant(UserFacingConstantsKey.FRUIT_COUNT_REACH_LIMIT)
                            .getDisplayName());
        }
        var fruit = fruitService.findById(fruitId);
        characterHelper.calculateCharacterBattleIndex(character, List.of(fruit));
        // this character can take more fruit
        var characterToFruitEntity =
                CharacterToFruitEntityBuilder.newBuilder()
                        .setCharacterId(characterId)
                        .setFruitId(fruitId)
                        .setWakeUp(false)
                        .build();
        characterToFruitDao.insert(characterToFruitEntity);
        return response.fruitMessage(
                UserFacingConstants.getConstant(UserFacingConstantsKey.FRUIT_EATEN_SUCCESS)
                        .getDisplayName());
    }

    public ListCharacterResponse listCharacters(ListCharacterRequest request) {
        var userId = UserHelper.getCurrentUserId();
        var resourceIds = userService.getResourceIdsByUserIdResourceType(userId, RESOURCE_TYPE);
        var totalSize = characterDao.count(request.getCharacterFilter(), resourceIds);
        // pageIndex is 1- based. offset is 0- based.
        var offset = (request.getPageIndex() - 1) * request.getPageSize();
        if (offset >= totalSize || offset < 0) {
            return new ListCharacterResponse().characters(List.of()).totalSize(totalSize);
        }
        var characterList =
                characterDao
                        .list(
                                request.getCharacterFilter(),
                                request.getSortKeys(),
                                offset,
                                request.getPageSize(),
                                resourceIds)
                        .stream()
                        .map(CharacterEntityConverter.INSTANCE::toCharacterListView)
                        .toList();
        return new ListCharacterResponse().characters(characterList).totalSize(totalSize);
    }

    public CharacterEntity getCharacterEntityById(UUID characterId) {
        return characterDao
                .findById(characterId)
                .orElseThrow(ExceptionUtils.notFound("Character with id: %s not found", characterId));
    }

    public CharacterEntity findByCharacterId(UUID characterId) {
        return characterDao.findById(characterId).orElseThrow();
    }

    public void updateCharacterEntity(CharacterEntity characterEntity) {
        characterDao.update(characterEntity);
    }

    public UpdateCharacterResponse updateCharacter(UpdateCharacterRequest character) {
        var characterUpdated = CharacterEntityConverter.INSTANCE.toCharacterEntity(character);
        var fruits = fruitService.listFruitEntityByCharacterId(character.getId());
        characterHelper.calculateCharacterBattleIndex(characterUpdated, fruits);
        updateHaki(character.getId(), character.getHakis());
        var response = new UpdateCharacterResponse();
        return response.result(
                UserFacingConstants.getConstant(UserFacingConstantsKey.UPDATE_SUCCESS)
                        .getDisplayName()).success(Boolean.TRUE);
    }

    public List<CharacterEntity> listNoUriCharacters() {
        return characterDao.listNoUriCharacters();
    }

    public CharacterEntity getPreviousCharacter(Long currentCreationRecordId) {
        var characterIdList = characterDao.getPreviousCharacterId(currentCreationRecordId);
        if (characterIdList.isEmpty()) {
            return null;
        }
        return characterDao.findById(characterIdList.get(0)).orElseThrow();
    }

    public CharacterEntity getNextCharacter(Long currentCreationRecordId) {
        var characterIdList = characterDao.getNextCharacterId(currentCreationRecordId);
        if (characterIdList.isEmpty()) {
            return null;
        }
        return characterDao.findById(characterIdList.get(0)).orElseThrow();
    }

    public void updateBountyAgeAndHeight() {
        characterDao.updateBounty();
        characterDao.updateAge();
        characterDao.updateHeight();
    }
}

