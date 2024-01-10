package com.yichi.onepiece.user.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.iam.model.TokenEntity;
import com.yichi.onepiece.model.User;
import com.yichi.onepiece.utils.EncryptHelper;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = EntityBuilder.class)
public interface UserConverter {
  UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

  @Mapping(target = "creationRecordId", ignore = true)
  @Mapping(target = "modificationRecordId", ignore = true)
  @Mapping(target = "encryptedPassword", ignore = true)
  UserEntity toUserEntity(User user);

  @Mapping(target = "loginRoleResourcesPermissionMap", ignore = true)
  @Mapping(target = "creationTimestamp", source = "creationRecord.actionTimestamp")
  @Mapping(target = "modificationTimestamp", source = "modificationRecord.actionTimestamp")
  User toUser(UserDetailImpl userDetail);

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

  default List<String> tokenEntityToToken(List<TokenEntity> tokenEntityList) {
    if (tokenEntityList == null) {
      return List.of();
    }
    return tokenEntityList.stream().map(TokenEntity::getTokenValue).toList();
  }
}
