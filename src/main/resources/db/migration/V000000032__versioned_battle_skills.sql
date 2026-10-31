-- Profiles are append-only. A battle stores its own complete rules/actor/skill snapshot.
CREATE TABLE character_combat_profile (
    character_id BINARY(16) NOT NULL,
    version INT NOT NULL,
    payload MEDIUMTEXT NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY(character_id, version),
    CONSTRAINT fk_combat_profile_character FOREIGN KEY(character_id) REFERENCES `character`(id)
);
CREATE TABLE battle_combat_record (
    battle_id BINARY(16) NOT NULL PRIMARY KEY,
    engine_version VARCHAR(32) NOT NULL,
    seed BIGINT NOT NULL,
    snapshot MEDIUMTEXT NOT NULL,
    events MEDIUMTEXT NULL,
    created_at BIGINT NOT NULL,
    CONSTRAINT fk_combat_record_battle FOREIGN KEY(battle_id) REFERENCES battle(id)
);
