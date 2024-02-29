alter table battle_process_detail drop column attacker_magic_loss;
alter table battle_process_detail  modify column defender_blood_loss int not null;