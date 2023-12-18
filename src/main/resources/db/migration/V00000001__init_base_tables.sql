-- THIS IS THE DB GOLDEN SCHEMA FOR UNIT TEST.

-- user
CREATE TABLE `user`(
    `id` BINARY(16) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `organization` VARCHAR(255),
    `login_role` VARCHAR(255) NOT NULL,
    `encrypted_password` VARCHAR(100) NOT NULL,
    `account_status` VARCHAR(100) NOT NULL,
    `profile_image` MEDIUMBLOB,
    `locale` VARCHAR(225) NOT NULL,
    `email_verified` TINYINT NOT NULL,
    `creation_record_id` BIGINT,
    `modification_record_id` BIGINT
);
ALTER TABLE `user` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `user` ADD CONSTRAINT `unique_name`
  UNIQUE(`name`);

-- token
CREATE TABLE `token`(
    `id` BINARY(16) NOT NULL,
    `token_value` VARCHAR(255) NOT NULL,
    `token_type` VARCHAR(255) NOT NULL,
    `revoked` TINYINT NOT NULL,
    `expired` TINYINT NOT NULL,
    `user_id` BINARY(16) NOT NULL
);
ALTER TABLE `token` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `token` ADD CONSTRAINT `fk_token__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);

-- user_resource
CREATE TABLE `user_resource`(
    `id` BINARY(16) NOT NULL,
    `user_id` BINARY(16),
    `source_user_id` BINARY(16),
    `resource_type` VARCHAR(255),
    `resource_id` BINARY(16) COMMENT 'resource id comes from character, battle, fruit and group'
);
ALTER TABLE `user_resource` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `user_resource` ADD CONSTRAINT `fk_user_resource__source_user_id`
  FOREIGN KEY(`source_user_id`) REFERENCES `user`(`id`);
ALTER TABLE `user_resource` ADD CONSTRAINT `fk_user_resource__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);

-- role
CREATE TABLE `role`(
    `id` BINARY(16) NOT NULL,
    `name` VARCHAR(255) NOT NULL
);
ALTER TABLE `role` ADD CONSTRAINT PRIMARY KEY(`id`);

-- user_role
CREATE TABLE `user_role`(
    `id` BINARY(16) NOT NULL,
    `user_id` BINARY(16),
    `role_id` BINARY(16)
);
ALTER TABLE `user_role` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `user_role` ADD CONSTRAINT `fk_user_role__role_id`
  FOREIGN KEY(`role_id`) REFERENCES `role`(`id`);
ALTER TABLE `user_role` ADD CONSTRAINT `fk_user_role__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);

-- resource_permission
CREATE TABLE `resource_permission` (
    `id` BINARY(16) NOT NULL,
    `resource_type` VARCHAR(100) NOT NULL,
    `permission` VARCHAR(100) NOT NULL
);
ALTER TABLE `resource_permission` ADD CONSTRAINT PRIMARY KEY(`id`);

-- role_resource
CREATE TABLE `role_to_resource_permission`(
    `id` BINARY(16) NOT NULL,
    `role_id` BINARY(16) NOT NULL,
    `resource_permission_id` BINARY(16) NOT NULL
);
ALTER TABLE `role_to_resource_permission` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `role_to_resource_permission` ADD CONSTRAINT `fk_role_to_resource_permission__resource_permission_id`
  FOREIGN KEY(`resource_permission_id`) REFERENCES `resource_permission`(`id`);
ALTER TABLE `role_to_resource_permission` ADD CONSTRAINT `fk_role_to_resource_permission__role_id`
  FOREIGN KEY(`role_id`) REFERENCES `role`(`id`);

-- action_lineage
CREATE TABLE `action_lineage`(
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `action_timestamp` BIGINT NOT NULL COMMENT 'the action timestamp in milliseconds',
    `user_id` BINARY(16) NOT NULL COMMENT 'either a user or a service account id',
    `user_name` VARCHAR(255) NOT NULL COMMENT 'the account display name at the time of the action',
    `role_id` BINARY(16) COMMENT 'role Id',
    `role_name` VARCHAR(255) COMMENT 'role name, eg: NIKA, ADMIN'
);
ALTER TABLE `action_lineage` ADD CONSTRAINT `fk_action_lineage__user_id`
    FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
ALTER TABLE `action_lineage` ADD CONSTRAINT `fk_action_lineage__role_id`
    FOREIGN KEY(`role_id`) REFERENCES `role`(`id`);

-- group
CREATE TABLE `group` (
    `id` BINARY(16) NOT NULL,
    `user_id` BINARY(16) NOT NULL,
    `role_id` BINARY(16) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `profile_image` MEDIUMBLOB,
    # action lineage
    `creation_record_id` BIGINT NOT NULL,
    `modification_record_id` BIGINT NOT NULL
);
ALTER TABLE `group` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `group` ADD CONSTRAINT `fk_group__user_id`
    FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
ALTER TABLE `group` ADD CONSTRAINT `fk_group__role_id`
    FOREIGN KEY(`role_id`) REFERENCES `role`(`id`);
ALTER TABLE `group` ADD CONSTRAINT `fk_group__creation_record_id`
    FOREIGN KEY(`creation_record_id`) REFERENCES `action_lineage`(`id`);
ALTER TABLE `group` ADD CONSTRAINT `fk_group__modification_record_id`
    FOREIGN KEY(`modification_record_id`) REFERENCES `action_lineage`(`id`);

-- character
CREATE TABLE `character`(
    `id` BINARY(16) NOT NULL,
    `user_id` BINARY(16) COMMENT 'Creator id',
    # basic information
    `name` VARCHAR(30) NOT NULL,
    `profile_image` MEDIUMBLOB,
    `maximum_fruit_count` TINYINT NOT NULL,
    `age` INT,
    `height` INT,
    `birth_place` VARCHAR(100),
    `date_of_birth` DATE,
    `hakis`  TEXT,
    `bounty` INT,
    # battle information
    `attack` INT NOT NULL,
    `defense` INT NOT NULL,
    `intelligence` INT NOT NULL,
    `speed` INT NOT NULL,
    `blood` INT NOT NULL,
    `energy` INT NOT NULL,
    `group_gain` DOUBLE NOT NULL,
    `system_resource` BOOLEAN NOT NULL,
    # action lineage
    `creation_record_id` BIGINT NOT NULL,
    `modification_record_id` BIGINT NOT NULL
);
ALTER TABLE `character` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `character` ADD CONSTRAINT `fk_character__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
ALTER TABLE `character` ADD CONSTRAINT `fk_character__creation_record_id`
  FOREIGN KEY(`creation_record_id`) REFERENCES `action_lineage`(`id`);
ALTER TABLE `character` ADD CONSTRAINT `fk_character__modification_record_id`
  FOREIGN KEY(`modification_record_id`) REFERENCES `action_lineage`(`id`);

CREATE TABLE `character_type`(
    `id` BINARY(16) NOT NULL,
    `character_id` BINARY(16) NOT NULL,
    `character_type` VARCHAR(255) NOT NULL
);
ALTER TABLE `character_type` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `character_type` ADD CONSTRAINT `fk_character_type__character_id`
    FOREIGN KEY(`character_id`) REFERENCES `character`(`id`);

CREATE TABLE `haki`(
     `id` BINARY(16) NOT NULL,
     `character_id` BINARY(16) NOT NULL,
     `haki` VARCHAR(255) NOT NULL
);
ALTER TABLE `haki` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `haki` ADD CONSTRAINT `fk_haki__character_id`
    FOREIGN KEY(`character_id`) REFERENCES `character`(`id`);

-- fruit
CREATE TABLE `fruit`(
    `id` BINARY(16) NOT NULL,
    `user_id` BINARY(16),
    `role_id` BINARY(16),
    `name` VARCHAR(255) NOT NULL,
    `fruit_type` VARCHAR(40) NOT NULL,
    `energy_consume` INT NOT NULL,
    `group_gain_strengthen` INT NOT NULL,
    `attack_strengthen` INT NOT NULL,
    `defense_strengthen` INT NOT NULL,
    `intelligence_strengthen` INT NOT NULL,
    `blood_strengthen` INT NOT NULL,
    `profile_image` MEDIUMBLOB,
    `system_resource` BOOLEAN NOT NULL,
    `creation_record_id` BIGINT NOT NULL,
    `modification_record_id` BIGINT NOT NULL,
    `speed_strengthen` INT NOT NULL
);
ALTER TABLE `fruit` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `fruit` ADD CONSTRAINT `fk_fruit__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
ALTER TABLE `fruit` ADD CONSTRAINT `fk_fruit__role_id`
    FOREIGN KEY(`role_id`) REFERENCES `role`(`id`);
ALTER TABLE `fruit` ADD CONSTRAINT `fk_fruit__creation_record_id`
  FOREIGN KEY(`creation_record_id`) REFERENCES `action_lineage`(`id`);
ALTER TABLE `fruit` ADD CONSTRAINT `fk_fruit__modification_record_id`
  FOREIGN KEY(`modification_record_id`) REFERENCES `action_lineage`(`id`);


CREATE TABLE `character_to_group` (
    `id` BINARY(16),
    `character_id` BINARY(16) NOT NULL,
    `group_id` BINARY(16) NOT NULL,
    `once_belonged_to` TINYINT NOT NULL
);
ALTER TABLE `character_to_group` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `character_to_group` ADD CONSTRAINT `fk_character_to_group__character_id`
    FOREIGN KEY(`character_id`) REFERENCES `character`(`id`);
ALTER TABLE `character_to_group` ADD CONSTRAINT `fk_character_to_group__group_id`
    FOREIGN KEY(`group_id`) REFERENCES `group`(`id`);

-- character_to_fruit
CREATE TABLE `character_to_fruit`(
    `id` BINARY(16) NOT NULL,
    `character_id` BINARY(16) NOT NULL,
    `fruit_id` BINARY(16) NOT NULL,
    `wake_up` BOOLEAN NOT NULL
);
ALTER TABLE `character_to_fruit` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `character_to_fruit` ADD CONSTRAINT `fk_character_to_fruit__character_id`
  FOREIGN KEY(`character_id`) REFERENCES `character`(`id`);
ALTER TABLE `character_to_fruit` ADD CONSTRAINT `fk_character_to_fruit__fruit_id`
  FOREIGN KEY(`fruit_id`) REFERENCES `fruit`(`id`);


-- battle
CREATE TABLE `battle`(
    `id` BINARY(16) NOT NULL,
    `user_id` BINARY(16) NOT NULL,
    `starter_id` BINARY(16) NOT NULL,
    `receiver_id` BINARY(16) NOT NULL,
    `creation_record_id` BIGINT NOT NULL,
    `modification_record_id` BIGINT NOT NULL,
    `type` VARCHAR(255) NOT NULL,
    `status` VARCHAR(255) NOT NULL,
    `winner_id` BINARY(16)
);
ALTER TABLE `battle` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `battle` ADD CONSTRAINT `fk_battle__starter_id`
  FOREIGN KEY(`starter_id`) REFERENCES `character`(`id`);
ALTER TABLE `battle` ADD CONSTRAINT `fk_battle__receiver_id`
  FOREIGN KEY(`receiver_id`) REFERENCES `character`(`id`);
ALTER TABLE `battle` ADD CONSTRAINT `fk_battle__winner_id`
  FOREIGN KEY(`winner_id`) REFERENCES `character`(`id`);
ALTER TABLE `battle` ADD CONSTRAINT `fk_battle__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
ALTER TABLE `battle` ADD CONSTRAINT `fk_battle__creation_record_id`
  FOREIGN KEY(`creation_record_id`) REFERENCES `action_lineage`(`id`);
ALTER TABLE `battle` ADD CONSTRAINT `fk_battle__modification_record_id`
  FOREIGN KEY(`modification_record_id`) REFERENCES `action_lineage`(`id`);

-- battle_process_detail
CREATE TABLE `battle_process_detail`(
    `id` BINARY(16) NOT NULL,
    `battle_id` BINARY(16) NOT NULL,
    `battle_process_round_number` BIGINT NOT NULL,
    `attack_missed` BINARY NOT NULL,
    `attacker_magic_loss` INT NOT NULL,
    `attacker_energy_loss` INT NOT NULL,
    `defender_energy_loss` INT,
    `defender_blood_loss` VARCHAR(255) NOT NULL,
    `attack_skill` VARCHAR(255) NOT NULL
);
ALTER TABLE `battle_process_detail` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `battle_process_detail` ADD CONSTRAINT `fk_battle_process_detail__battle_id`
  FOREIGN KEY(`battle_id`) REFERENCES `battle`(`id`);

-- battle_status_detail
CREATE TABLE `battle_status_detail`(
    `id` BINARY(16) NOT NULL,
    `battle_id` BINARY(16) NOT NULL,
    `battle_status_round_number` INT NOT NULL,
    `attack_side` VARCHAR(255) NOT NULL,
    `attacker` TEXT NOT NULL,
    `defender` TEXT NOT NULL
);
ALTER TABLE `battle_status_detail` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `battle_status_detail` ADD CONSTRAINT `fk_battle_status_detail__battle_id`
  FOREIGN KEY(`battle_id`) REFERENCES `battle`(`id`);

-- issue
CREATE TABLE `issue`(
    `id` BIGINT NOT NULL,
    `creation_user_id` BINARY(16) NOT NULL,
    `issue_type` VARCHAR(255) NOT NULL,
    `issue_header` VARCHAR(255) NOT NULL,
    `issue_description` VARCHAR(255) NOT NULL,
    `issue_status` VARCHAR(255) NOT NULL,
    `creation_timestamp` BIGINT NOT NULL,
    `modification_timestamp` BIGINT NOT NULL
);
ALTER TABLE `issue` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `issue` ADD CONSTRAINT `fk_issue__creation_user_id`
  FOREIGN KEY(`creation_user_id`) REFERENCES `user`(`id`);

-- email_verification
CREATE TABLE `email_verification`(
    `id` BINARY(16) NOT NULL,
    `user_id` BINARY(16) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `email_verification_code` VARCHAR(255) NOT NULL,
    `is_used` BOOLEAN NOT NULL,
    `creation_timestamp` BIGINT NOT NULL,
    `expiration_timestamp` BIGINT NOT NULL
);
ALTER TABLE `email_verification` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `email_verification` ADD CONSTRAINT `fk_email_verification__user_id`
  FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);


-- file_object
CREATE TABLE `file_object`(
    `id` BINARY(16) NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `resource` BINARY(16),
    `creator_id` BINARY(16),
    `creation_record_id` BIGINT NOT NULL,
    `file_category` VARCHAR(255) NOT NULL,
    `http_mime_type` VARCHAR(255) NOT NULL,
    `oss_uri` VARCHAR(255) NOT NULL
);
ALTER TABLE `file_object` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `file_object` ADD CONSTRAINT `fk_file_object__creation_record_id`
  FOREIGN KEY(`creation_record_id`) REFERENCES `action_lineage`(`id`);
ALTER TABLE `file_object` ADD CONSTRAINT `fk_file_creator_user_id__user_id`
  FOREIGN KEY(`creator_id`) REFERENCES `user`(`id`);

-- battle_list_view
CREATE VIEW `view_battle_list_view`
AS
SELECT
   `battle`.*,
   `creation_record`.user_name as creator,
   `creation_record`.role_name,
   `creation_record`.action_timestamp as creation_timestamp,
   `modification_record`.action_timestamp as modification_timestamp,
   `starter`.name as starter_name,
   `starter`.profile_image as starter_image,
   `receiver`.name as receiver_name,
   `receiver`.profile_image as receiver_image
FROM `battle` JOIN `character` AS `starter` on battle.starter_id = starter.id
JOIN `character` AS `receiver` on battle.receiver_id = receiver.id
JOIN `action_lineage` as creation_record on creation_record.id = battle.creation_record_id
JOIN `action_lineage` as modification_record on modification_record.id = battle.modification_record_id;

-- character_list_view
CREATE VIEW `view_character_list_view`
AS
SELECT
    `character`.*,
    `creation_record`.action_timestamp as creation_timestamp,
    `modification_record`.action_timestamp as modification_timestamp,
    `creation_record`.user_name as creator,
    `creation_record`.role_name,
    `creation_record`.role_id
FROM `character`
JOIN `action_lineage` as creation_record on creation_record.id = `character`.creation_record_id
JOIN `action_lineage` as modification_record on modification_record.id = `character`.modification_record_id
LEFT JOIN `character_type` on `character`.`id` = `character_type`.`character_id`
LEFT JOIN `haki` on `character`.id = haki.character_id;

-- fruit_list_view
CREATE VIEW `view_fruit_list_view`
AS
SELECT
    `fruit`.*,
    `creation_record`.action_timestamp as creation_timestamp,
    `creation_record`.user_name as creator,
    `creation_record`.role_name,
    `modification_record`.action_timestamp as modification_timestamp
FROM `fruit`
JOIN `action_lineage` as creation_record on creation_record.id = `fruit`.creation_record_id
JOIN `action_lineage` as modification_record on modification_record.id = `fruit`.modification_record_id;
