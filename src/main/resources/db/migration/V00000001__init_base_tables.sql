-- user
CREATE TABLE user (
    id BINARY(16) PRIMARY KEY,
    `name` VARCHAR(255),
    email VARCHAR(255),
    organization VARCHAR(255),
    login_role VARCHAR(255),
    encrypted_password VARCHAR(100),
    account_condition VARCHAR(100),
    profile_image BLOB
);

-- role
CREATE TABLE role (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(255)
);

-- resource
CREATE TABLE resource (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(255)
);

-- role_resource
CREATE TABLE role_resource (
     id BINARY(16) PRIMARY KEY,
     role_id BINARY(16),
     resource_id BINARY(16)
);
ALTER TABLE role_resource ADD CONSTRAINT fk_role_resource_role_id__role_id
FOREIGN KEY(role_id) REFERENCES role(id);
ALTER TABLE role_resource ADD CONSTRAINT fk_role_resource_resource_id__resource_id
    FOREIGN KEY(resource_id) REFERENCES resource(id);

-- user_role
CREATE TABLE user_role (
     id BINARY(16) PRIMARY KEY,
     user_id BINARY(16),
     role_id BINARY(16)
);
ALTER TABLE user_role ADD CONSTRAINT fk_user_role_role_id__role_id
    FOREIGN KEY(role_id) REFERENCES role(id);
ALTER TABLE user_role ADD CONSTRAINT fk_user_role_user_id__user_id
    FOREIGN KEY(user_id) REFERENCES user(id);

-- character
CREATE TABLE `character` (
    id BINARY(16) NOT NULL PRIMARY KEY,
    character_type VARCHAR(30) NOT NULL,
    profile_image LONGBLOB,
    name VARCHAR(30) NOT NULL,
    group_oid VARCHAR(30),
    has_fruits TINYINT NOT NULL,
    fruit_limit TINYINT NOT NULL,
    fruits TEXT NOT NULL,
    group_gain INT NOT NULL,
    attack INT NOT NULL,
    defense INT NOT NULL,
    intelligence INT NOT NULL,
    speed INT NOT NULL,
    blood INT NOT NULL,
    magic INT NOT NULL,
    user_id BINARY(16)
);
ALTER TABLE `character` ADD CONSTRAINT fk_character__user_id
    FOREIGN KEY(user_id) REFERENCES user(id);

-- battle
CREATE TABLE battle (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    starter_id BINARY(16) NOT NULL,
    receiver_id BINARY(16) NOT NULL
);
ALTER TABLE battle ADD CONSTRAINT fk_battle__character_id_initiator
    FOREIGN KEY(starter_id) REFERENCES `character`(id);
ALTER TABLE battle ADD CONSTRAINT fk_battle__character_id_receiver
    FOREIGN KEY(receiver_id) REFERENCES `character`(id);
ALTER TABLE battle ADD CONSTRAINT fk_battle__user_id
    FOREIGN KEY(user_id) REFERENCES user(id);


-- fruit
CREATE TABLE fruit (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_id BINARY(16),
    fruit_type VARCHAR(40) NOT NULL,
    magic_consume INT NOT NULL,
    group_gain_strengthen INT NOT NULL,
    attack_strengthen INT NOT NULL,
    defense_strengthen INT NOT NULL,
    intelligence_strengthen INT NOT NULL,
    blood_strengthen INT NOT NULL,
    user_id BINARY(16)
);
ALTER TABLE fruit ADD CONSTRAINT fk_fruit__owner_id
    FOREIGN KEY(owner_id) REFERENCES `character`(id);
ALTER TABLE fruit ADD CONSTRAINT fk_fruit__user_id
    FOREIGN KEY(user_id) REFERENCES user(id);
