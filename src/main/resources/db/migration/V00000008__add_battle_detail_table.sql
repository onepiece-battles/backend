create table battle_status_detail(
    id BINARY(16) PRIMARY KEY,
    battle_id BINARY(16) NOT NULL,
    battle_status_round_number INT NOT NULL,
    attack_side VARCHAR(255) NOT NULL,
    attacker TEXT NOT NULL,
    defender TEXT NOT NULL
);

create table battle_process_detail(
    id BINARY(16) PRIMARY KEY,
    battle_id BINARY(16) NOT NULL,
    battle_process_round_number BIGINT NOT NULL,
    attack_missed BINARY NOT NULL,
    attacker_magic_loss INT NOT NULL,
    attacker_energy_loss INT,
    defender_magic_loss INT NOT NULL,
    defender_energy_loss INT,
    defender_blood_loss VARCHAR(255) NOT NULL,
    attack_skill_type VARCHAR(255) NOT NULL
)