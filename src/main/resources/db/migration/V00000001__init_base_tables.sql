create table user(
      id BINARY(16) PRIMARY KEY,
      user_name VARCHAR(255) NOT NULL,
      organization TEXT NOT NULL,
      encrypted_password VARCHAR(255) NOT NULL,
      account_condition VARCHAR(40) NOT NULL
);

create table character_entity (
        id BINARY(16) PRIMARY KEY,
        character_types TEXT NOT NULL,
        profile_image LONGBLOB,
        name VARCHAR(30) NOT NULL,
        group_oid VARCHAR(30),
        has_fruits TINYINT NOT NULL,
        fruit_limit TINYINT NOT NULL,
        fruits TEXT,
        group_gain INT NOT NULL,
        attack INT NOT NULL,
        defense INT NOT NULL,
        intelligence INT NOT NULL,
        blood INT NOT NULL,
        magic INT NOT NULL
);

create table fruit (
       id BINARY(16) PRIMARY KEY,
       name VARCHAR(255) NOT NULL,
       owner_id BINARY(16),
       fruit_type VARCHAR(40) NOT NULL,
       magic_consume INT NOT NULL,
       group_gain_strengthen INT NOT NULL,
       attack_strengthen INT NOT NULL,
       defense_strengthen INT NOT NULL,
       intelligence_strengthen INT NOT NULL,
       blood_strengthen INT NOT NULL
);
ALTER TABLE fruit ADD CONSTRAINT fk_fruit__owner_id
    FOREIGN KEY(owner_id) REFERENCES character_entity(id);

create table battle (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        user_id BINARY(16) NOT NULL,
        character_id_initiator BINARY(16) NOT NULL,
        character_id_receiver BINARY(16) NOT NULL,
        action_timestamp BIGINT NOT NULL COMMENT 'the timestamp when this fight is created'
);
ALTER TABLE battle ADD CONSTRAINT fk_battle__user_id
    FOREIGN KEY(user_id) REFERENCES user(id);
ALTER TABLE battle ADD CONSTRAINT fk_battle__character_id_initiator
    FOREIGN KEY(character_id_initiator) REFERENCES character_entity(id);
ALTER TABLE battle ADD CONSTRAINT fk_battle__character_id_receiver
    FOREIGN KEY(character_id_receiver) REFERENCES character_entity(id);