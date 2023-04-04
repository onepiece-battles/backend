package com.yichi.onepiece.Iam;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PermissionDefinition {
    public static final Logger logger = LoggerFactory.getLogger(PermissionDefinition.class);

    public enum PermissionEnum {
        // character
        CHARACTER_CREATE,
        CHARACTER_READ,
        CHARACTER_LIST,
        CHARACTER_UPDATE,
        CHARACTER_DELETE,

        // fruit
        FRUIT_CREATE,
        FRUIT_READ,
        FRUIT_LIST,
        FRUIT_UPDATE,
        FRUIT_DELETE,

        // battle
        BATTLE_CREATE,
        BATTLE_READ,
        BATTLE_LIST,
        BATTLE_UPDATE,
        BATTLE_DELETE,
    }

    public List<String> getAllPermissions() {
        return Arrays.stream(PermissionEnum.values()).map(Enum::toString).collect(Collectors.toList());
    }
}
