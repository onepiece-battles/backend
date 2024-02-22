package com.yichi.onepiece.group.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.model.Group;
import com.yichi.onepiece.model.GroupFullView;
import com.yichi.onepiece.utils.EncryptHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {EntityBuilder.class})
public interface GroupConverter {
  GroupConverter INSTANCE = Mappers.getMapper(GroupConverter.class);

  com.yichi.onepiece.model.GroupListView toGroupListView(GroupListView groupListView);


  GroupFullView toGroupFullView(com.yichi.onepiece.group.model.GroupFullView groupFullView);

  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "systemResource", ignore = true)
  GroupEntity toGroupEntity(Group group);

  default String blobToString(Byte[] profileByteArray) {
    if (profileByteArray == null) {
      return null;
    }
    return EncryptHelper.base64Encode(ArrayUtils.toPrimitive(profileByteArray));
  }

  default Byte[] stringToBlob(String string) {
    if (string == null) {
      return null;
    }
    return ArrayUtils.toObject(EncryptHelper.base64Decode(string));
  }
}
