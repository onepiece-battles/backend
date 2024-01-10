package com.yichi.onepiece.group.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.model.GroupFullView;
import com.yichi.onepiece.utils.EncryptHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {EntityBuilder.class})
public interface GroupConverter {
  GroupConverter INSTANCE = Mappers.getMapper(GroupConverter.class);

  com.yichi.onepiece.model.GroupListView toGroupListView(GroupListView groupListView);


  GroupFullView toGroupFullView(com.yichi.onepiece.group.model.GroupFullView groupFullView);

  default String blobToString(Byte[] profileByteArray) {
    if (profileByteArray == null) {
      return null;
    }
    return EncryptHelper.base64Encode(ArrayUtils.toPrimitive(profileByteArray));
  }
}
