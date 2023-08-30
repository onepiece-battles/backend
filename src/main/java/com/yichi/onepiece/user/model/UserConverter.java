package com.yichi.onepiece.user.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.model.User;
import com.yichi.onepiece.utils.EncryptHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

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

  default String blobToString(Byte[] profileByteArray) {
    if (profileByteArray == null) {
      return null;
    }
    return EncryptHelper.base64Encode(ArrayUtils.toPrimitive(profileByteArray));
  }

  default Byte[] stringToBlob(String profileEncodedValue) {
    if (profileEncodedValue == null) {
      return null;
    }
    return ArrayUtils.toObject(EncryptHelper.base64Decode(profileEncodedValue));
  }
}
