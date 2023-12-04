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
    `status` VARCHAR(255) NOT NULL,
    `is_system_resource` BOOLEAN NOT NULL,
    `winner_id` BINARY(16)
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
ALTER TABLE `battle` ADD CONSTRAINT `fk_battle__character_id_winner`
  FOREIGN KEY(`winner_id`) REFERENCES `character`(`id`);
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
    `group_gain` INT NOT NULL,
    `attack` INT NOT NULL,
    `defense` INT NOT NULL,
    `intelligence` INT NOT NULL,
    `speed` INT NOT NULL,
    `blood` INT NOT NULL,
    `magic` INT NOT NULL,
    `user_id` BINARY(16),
    `creation_record_id` BIGINT NOT NULL,
    `modification_record_id` BIGINT NOT NULL,
    `is_system_resource` BOOLEAN NOT NULL
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

## character_to_fruit

```sql
CREATE TABLE `character_to_fruit`(
    `id` BINARY(16) NOT NULL,
    `character_id` BINARY(16) NOT NULL,
    `fruit_id` BINARY(16) NOT NULL,
    `wake_up` BOOLEAN NOT NULL
);
```

```sql
ALTER TABLE `character_to_fruit` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `character_to_fruit` ADD CONSTRAINT `fk_character_to_fruit__character_id`
  FOREIGN KEY(`character_id`) REFERENCES `character`(`id`);
```

```sql
ALTER TABLE `character_to_fruit` ADD CONSTRAINT `fk_character_to_fruit__fruit_id`
  FOREIGN KEY(`fruit_id`) REFERENCES `fruit`(`id`);
```

## email_verification

```sql
CREATE TABLE `email_verification`(
    `id` BINARY(16) NOT NULL,
    `user_id` BINARY(16) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `email_verification_code` VARCHAR(255) NOT NULL,
    `is_used` BOOLEAN NOT NULL,
    `creation_timestamp` BIGINT NOT NULL,
    `expiration_timestamp` BIGINT NOT NULL
);
```

```sql
ALTER TABLE `email_verification` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `email_verification` ADD CONSTRAINT `fk_email_verification_user_id__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
```

## file_object

```sql
CREATE TABLE `file_object`(
    `id` BINARY(16) NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `resource_id` BINARY(16),
    `creator_user_id` BINARY(16),
    `creation_record_id` BIGINT NOT NULL,
    `file_category` VARCHAR(255) NOT NULL,
    `http_mime_type` VARCHAR(255) NOT NULL,
    `oss_uri` VARCHAR(255) NOT NULL
);
```

```sql
ALTER TABLE `file_object` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `file_object` ADD CONSTRAINT `fk_file_creation_record_id__action_lineage_id`
  FOREIGN KEY(`creation_record_id`) REFERENCES `action_lineage`(`id`);
```

```sql
ALTER TABLE `file_object` ADD CONSTRAINT `fk_file_creator_user_id__user_id`
  FOREIGN KEY(`creator_user_id`) REFERENCES `user`(`id`);
```

## fruit

```sql
CREATE TABLE `fruit`(
    `id` BINARY(16) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
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
    `modification_record_id` BIGINT NOT NULL,
    `is_system_resource` BOOLEAN NOT NULL,
    `speed_strengthen` INT NOT NULL
);
```

```sql
ALTER TABLE `fruit` ADD CONSTRAINT PRIMARY KEY(`id`);
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

## issue

```sql
CREATE TABLE `issue`(
    `id` BIGINT NOT NULL,
    `creation_user_id` BIGINT NOT NULL,
    `issue_type` VARCHAR(255) NOT NULL,
    `issue_header` VARCHAR(255) NOT NULL,
    `issue_description` VARCHAR(255) NOT NULL,
    `issue_status` VARCHAR(255) NOT NULL,
    `creation_timestamp` BIGINT NOT NULL,
    `modification_timestamp` BIGINT NOT NULL
);
```

```sql
ALTER TABLE `issue` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `issue` ADD CONSTRAINT `fk_issue__creation_user_id`
  FOREIGN KEY(`creation_user_id`) REFERENCES `user`(`id`);
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
    `name` VARCHAR(255) NOT NULL
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
    `name` VARCHAR(255) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `organization` VARCHAR(255),
    `login_role` VARCHAR(255) NOT NULL,
    `encrypted_password` VARCHAR(100) NOT NULL,
    `account_status` VARCHAR(100) NOT NULL,
    `profile_image` MEDIUMBLOB NOT NULL,
    `locale` VARCHAR(225) NOT NULL,
    `email_verified` TINYINT NOT NULL,
    `creation_record_id` BIGINT,
    `modification_record_id` BIGINT
);
```

```sql
ALTER TABLE `user` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `user` ADD CONSTRAINT `unique_name`
  UNIQUE(`name`);
```

## user_resource

```sql
CREATE TABLE `user_resource`(
    `id` BINARY(16) NOT NULL,
    `user_id` BINARY(16),
    `source_user_id` BINARY(16),
    `resource_type` VARCHAR(255),
    `resource_id` BINARY(16)
);
```

```sql
ALTER TABLE `user_resource` ADD CONSTRAINT PRIMARY KEY(`id`);
```

```sql
ALTER TABLE `user_resource` ADD CONSTRAINT `fk_user_resource__source_user_id`
  FOREIGN KEY(`source_user_id`) REFERENCES `user`(`id`);
```

```sql
ALTER TABLE `user_resource` ADD CONSTRAINT `fk_user_resource__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
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
