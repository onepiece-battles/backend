create table character_book (
    id BINARY(16) PRIMARY KEY,
    character_types TEXT NOT NULL,
    profile_image LONGBLOB,
    name VARCHAR(30) NOT NULL,
    group_oid VARCHAR(30),
    has_fruit TINYINT NOT NULL,
    fruit_limit TINYINT NOT NULL,
    fruits TEXT,
    group_gain INT NOT NULL,
    attack INT NOT NULL,
    defense INT NOT NULL,
    intelligence INT NOT NULL,
    blood INT NOT NULL,
    magic INT NOT NULL
);