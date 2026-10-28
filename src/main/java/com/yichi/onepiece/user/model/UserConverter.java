package com.yichi.onepiece.user.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.iam.model.TokenEntity;
import com.yichi.onepiece.model.User;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = EntityBuilder.class)
public interface UserConverter {
  UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

  @Mapping(target = "creationRecordId", ignore = true)
  @Mapping(target = "modificationRecordId", ignore = true)
  @Mapping(target = "encryptedPassword", ignore = true)
  @Mapping(target = "imageUri", ignore = true)
  UserEntity toUserEntity(User user);

  @Mapping(target = "loginRoleResourcesPermissionMap", ignore = true)
  @Mapping(target = "profileImage", ignore = true)
  @Mapping(target = "tokens", ignore = true)
  User toUser(UserDetailImpl userDetail);

  @Mapping(target = "loginRoleResourcesPermissionMap", ignore = true)
  @Mapping(target = "profileImage", ignore = true)
  User toUser(UserEntity userEntity);

  default List<String> tokenEntityToToken(List<TokenEntity> tokenEntityList) {
    if (tokenEntityList == null) {
      return List.of();
    }
    return tokenEntityList.stream().map(TokenEntity::getTokenValue).toList();
  }
}
