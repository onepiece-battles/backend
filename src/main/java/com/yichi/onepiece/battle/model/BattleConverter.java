package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.character.model.converter.CharacterEntityConverter;
import com.yichi.onepiece.model.Battle;
import com.yichi.onepiece.model.BattleExecution;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import com.yichi.onepiece.utils.EncryptHelper;
import org.apache.commons.lang3.ArrayUtils;

@Mapper(uses = {EntityBuilder.class, CharacterEntityConverter.class})
public interface BattleConverter {
  BattleConverter INSTANCE = Mappers.getMapper(BattleConverter.class);

  com.yichi.onepiece.model.BattleListView toBattleListView(BattleListView battleEntity);

  Battle toBattle(BattleEntity battleEntity);

  com.yichi.onepiece.model.BattleFullView toBattleFullView(BattleFullView battleFullViewEntity);

  BattleExecution toBattleExecution(BattleExecutionEntity battleEntity);

  default Byte[] stringToBlob(String string) {
    if (string == null) {
      return null;
    }
    return ArrayUtils.toObject(EncryptHelper.base64Decode(string));
  }

  default String blobToString(Byte[] profileByteArray) {
    if (profileByteArray == null) {
      return null;
    }
    return EncryptHelper.base64Encode(ArrayUtils.toPrimitive(profileByteArray));
  }
}
