package com.yichi.onepiece.group.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.character.model.entity.CharacterInFruitView;
import com.yichi.onepiece.model.CharacterInGroupView;
import com.yichi.onepiece.model.Group;
import com.yichi.onepiece.model.GroupFullView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {EntityBuilder.class})
public interface GroupConverter {
  GroupConverter INSTANCE = Mappers.getMapper(GroupConverter.class);

  com.yichi.onepiece.model.GroupListView toGroupListView(GroupListView groupListView);


  GroupFullView toGroupFullView(com.yichi.onepiece.group.model.GroupFullView groupFullView);

  @Mapping(target = "systemResource", ignore = true)
  GroupEntity toGroupEntity(Group group);

  Group toGroup(GroupEntity group);

  CharacterInGroupView toCharacterInGroupView(com.yichi.onepiece.character.model.entity.CharacterInGroupView characterInGroupView);
}
