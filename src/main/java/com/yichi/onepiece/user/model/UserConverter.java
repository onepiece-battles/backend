package com.yichi.onepiece.user.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.model.User;
import org.apache.commons.lang3.ArrayUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

@Mapper(uses = EntityBuilder.class)
public interface UserConverter {
  UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

  @Mapping(target = "tokens", ignore = true)
  @Mapping(target = "loginRoleResources", ignore = true)
  @Mapping(target = "userRoles", ignore = true)
  User toUser(UserEntity userEntity);

  @Mapping(target = "encryptedPassword", ignore = true)
  @Mapping(target = "accountCondition", ignore = true)
  UserEntity toUserEntity(User user);

  default Resource blobToResource(Byte[] profileByteArray) {
    if (profileByteArray == null) {
      return null;
    }
    return new ByteArrayResource(ArrayUtils.toPrimitive(profileByteArray));
  }
}
