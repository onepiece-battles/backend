# DB GOLDEN SCHEMA

## action_lineage

```sql
CREATE TABLE `action_lineage`(
    `id` BIGINT AUTO_INCREMENT,
    `action_timestamp` BIGINT NOT NULL COMMENT 'the action timestamp in milliseconds',
    `user_id` BINARY(16) NOT NULL COMMENT 'either a user or a service account id',
    `user_name` VARCHAR(255) NOT NULL COMMENT 'the account display name at the time of the action',
    `role_id` BINARY(16) COMMENT 'role Id',
    `role_name` VARCHAR(255) COMMENT 'role name, eg: NIKA, ADMIN'
);
```

```sql
ALTER TABLE `action_lineage` ADD CONSTRAINT PRIMARY KEY(`id`);
```

## battle

```sql
CREATE TABLE `battle`(
    `id` BINARY(16) NOT NULL,
    `user_id` BINARY(16) NOT NULL,
    `starter_id` BINARY(16) NOT NULL,
    `receiver_id` BINARY(16) NOT NULL,
    `creation_record_id` BIGINT NOT NULL,
    `modification_record_id` BIGINT NOT NULL,
    `type` VARCHAR(255) NOT NULL,
    `status` VARCHAR(255) NOT NULL
);
```

```sql
ALTER TABLE `battle` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `battle` ADD CONSTRAINT `fk_battle__character_id_initiator`
  FOREIGN KEY(`starter_id`) REFERENCES `character`(`id`);
```

```sql
ALTER TABLE `battle` ADD CONSTRAINT `fk_battle__character_id_receiver`
  FOREIGN KEY(`receiver_id`) REFERENCES `character`(`id`);
```

```sql
ALTER TABLE `battle` ADD CONSTRAINT `fk_battle__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
```

```sql
ALTER TABLE `battle` ADD CONSTRAINT `fk_battle_creation_record_id__action_lineage_id`
  FOREIGN KEY(`creation_record_id`) REFERENCES `action_lineage`(`id`);
```

```sql
ALTER TABLE `battle` ADD CONSTRAINT `fk_battle_modification_record_id__action_lineage_id`
  FOREIGN KEY(`modification_record_id`) REFERENCES `action_lineage`(`id`);
```

## battle_process_detail

```sql
CREATE TABLE `battle_process_detail`(
    `id` BINARY(16) NOT NULL,
    `battle_id` BINARY(16) NOT NULL,
    `battle_process_round_number` BIGINT NOT NULL,
    `attack_missed` BINARY NOT NULL,
    `attacker_magic_loss` INT NOT NULL,
    `attacker_energy_loss` INT,
    `defender_magic_loss` INT NOT NULL,
    `defender_energy_loss` INT,
    `defender_blood_loss` VARCHAR(255) NOT NULL,
    `attack_skill_type` VARCHAR(255) NOT NULL
);
```

```sql
ALTER TABLE `battle_process_detail` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `battle_process_detail` ADD CONSTRAINT `fk_battle_process_detail__battle_id`
  FOREIGN KEY(`battle_id`) REFERENCES `battle`(`id`);
```

## battle_status_detail

```sql
CREATE TABLE `battle_status_detail`(
    `id` BINARY(16) NOT NULL,
    `battle_id` BINARY(16) NOT NULL,
    `battle_status_round_number` INT NOT NULL,
    `attack_side` VARCHAR(255) NOT NULL,
    `attacker` TEXT NOT NULL,
    `defender` TEXT NOT NULL
);
```

```sql
ALTER TABLE `battle_status_detail` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `battle_status_detail` ADD CONSTRAINT `fk_battle_status_detail__battle_id`
  FOREIGN KEY(`battle_id`) REFERENCES `battle`(`id`);
```

## character

```sql
CREATE TABLE `character`(
    `id` BINARY(16) NOT NULL,
    `character_type` VARCHAR(30) NOT NULL,
    `profile_image` MEDIUMBLOB NOT NULL,
    `name` VARCHAR(30) NOT NULL,
    `group_oid` VARCHAR(30),
    `has_fruits` TINYINT NOT NULL,
    `fruit_limit` TINYINT NOT NULL,
    `fruits` TEXT NOT NULL,
    `group_gain` INT NOT NULL,
    `attack` INT NOT NULL,
    `defense` INT NOT NULL,
    `intelligence` INT NOT NULL,
    `speed` INT NOT NULL,
    `blood` INT NOT NULL,
    `magic` INT NOT NULL,
    `user_id` BINARY(16),
    `creation_record_id` BIGINT NOT NULL,
    `modification_record_id` BIGINT NOT NULL
);
```

```sql
ALTER TABLE `character` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `character` ADD CONSTRAINT `fk_character__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
```

```sql
ALTER TABLE `character` ADD CONSTRAINT `fk_character_creation_record_id__action_lineage_id`
  FOREIGN KEY(`creation_record_id`) REFERENCES `action_lineage`(`id`);
```

```sql
ALTER TABLE `character` ADD CONSTRAINT `fk_character_modification_record_id__action_lineage_id`
  FOREIGN KEY(`modification_record_id`) REFERENCES `action_lineage`(`id`);
```

## fruit

```sql
CREATE TABLE `fruit`(
    `id` BINARY(16) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `owner_id` BINARY(16),
    `fruit_type` VARCHAR(40) NOT NULL,
    `magic_consume` INT NOT NULL,
    `group_gain_strengthen` INT NOT NULL,
    `attack_strengthen` INT NOT NULL,
    `defense_strengthen` INT NOT NULL,
    `intelligence_strengthen` INT NOT NULL,
    `blood_strengthen` INT NOT NULL,
    `user_id` BINARY(16),
    `profile_image` MEDIUMBLOB NOT NULL,
    `creation_record_id` BIGINT NOT NULL,
    `modification_record_id` BIGINT NOT NULL
);
```

```sql
ALTER TABLE `fruit` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `fruit` ADD CONSTRAINT `fk_fruit__owner_id`
  FOREIGN KEY(`owner_id`) REFERENCES `character`(`id`);
```

```sql
ALTER TABLE `fruit` ADD CONSTRAINT `fk_fruit__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
```

```sql
ALTER TABLE `fruit` ADD CONSTRAINT `fk_fruit_creation_record_id__action_lineage_id`
  FOREIGN KEY(`creation_record_id`) REFERENCES `action_lineage`(`id`);
```

```sql
ALTER TABLE `fruit` ADD CONSTRAINT `fk_fruit_modification_record_id__action_lineage_id`
  FOREIGN KEY(`modification_record_id`) REFERENCES `action_lineage`(`id`);
```

## resource

```sql
CREATE TABLE `resource`(
    `id` BINARY(16) NOT NULL,
    `name` VARCHAR(255),
    `permissions` TEXT NOT NULL
);
```

```sql
ALTER TABLE `resource` ADD CONSTRAINT PRIMARY KEY(`id`);
```

## role

```sql
CREATE TABLE `role`(
    `id` BINARY(16) NOT NULL,
    `name` VARCHAR(255)
);
```

```sql
ALTER TABLE `role` ADD CONSTRAINT PRIMARY KEY(`id`);
```

## role_resource

```sql
CREATE TABLE `role_resource`(
    `id` BINARY(16) NOT NULL,
    `role_id` BINARY(16),
    `resource_id` BINARY(16),
    `permissions` TEXT NOT NULL
);
```

```sql
ALTER TABLE `role_resource` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `role_resource` ADD CONSTRAINT `fk_role_resource_resource_id__resource_id`
  FOREIGN KEY(`resource_id`) REFERENCES `resource`(`id`);
```

```sql
ALTER TABLE `role_resource` ADD CONSTRAINT `fk_role_resource_role_id__role_id`
  FOREIGN KEY(`role_id`) REFERENCES `role`(`id`);
```

## token

```sql
CREATE TABLE `token`(
    `id` BINARY(16) NOT NULL,
    `token_value` VARCHAR(255) NOT NULL,
    `token_type` VARCHAR(255) NOT NULL,
    `revoked` TINYINT NOT NULL,
    `expired` TINYINT NOT NULL,
    `user_id` BINARY(16) NOT NULL
);
```

```sql
ALTER TABLE `token` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `token` ADD CONSTRAINT `fk_token_user_id__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
```

## user

```sql
CREATE TABLE `user`(
    `id` BINARY(16) NOT NULL,
    `name` VARCHAR(255),
    `email` VARCHAR(255),
    `organization` VARCHAR(255),
    `login_role` VARCHAR(255),
    `encrypted_password` VARCHAR(100),
    `account_condition` VARCHAR(100),
    `profile_image` MEDIUMBLOB NOT NULL
);
```

```sql
ALTER TABLE `user` ADD CONSTRAINT PRIMARY KEY(`id`);
```

## user_role

```sql
CREATE TABLE `user_role`(
    `id` BINARY(16) NOT NULL,
    `user_id` BINARY(16),
    `role_id` BINARY(16)
);
```

```sql
ALTER TABLE `user_role` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `user_role` ADD CONSTRAINT `fk_user_role_role_id__role_id`
  FOREIGN KEY(`role_id`) REFERENCES `role`(`id`);
```

```sql
ALTER TABLE `user_role` ADD CONSTRAINT `fk_user_role_user_id__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
```
