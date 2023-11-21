alter table battle drop column  winner_name;
alter table battle add column winner_id binary(16);

ALTER TABLE battle ADD CONSTRAINT fk_battle__character_id_winner
    FOREIGN KEY(winner_id) REFERENCES `character`(id);