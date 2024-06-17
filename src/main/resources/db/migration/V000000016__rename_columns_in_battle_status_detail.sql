alter table battle_status_detail change attacker init_side text;
alter table battle_status_detail change defender receive_side text;
alter table battle_status_detail drop column attacker_id;
alter table battle_status_detail drop column attack_side;

