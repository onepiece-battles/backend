create table fruit (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_id BINARY(16),
    fruit_type VARCHAR(40) NOT NULL,
    magic_consume INT NOT NULL,
    getGroupGainStrengthen INT NOT NULL,
    attack_strengthen INT NOT NULL,
    defense_strengthen INT NOT NULL,
    intelligence_strengthen INT NOT NULL,
    blood_strengthen INT NOT NULL
);
ALTER TABLE fruit ADD CONSTRAINT fk_fruit__owner_id
    FOREIGN KEY(owner_id) REFERENCES character_book(id);