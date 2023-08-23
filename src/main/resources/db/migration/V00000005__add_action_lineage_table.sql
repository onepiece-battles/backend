create table action_lineage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_timestamp BIGINT NOT NULL COMMENT 'the action timestamp in milliseconds',
    user_id BINARY(16) NOT NULL COMMENT 'either a user or a service account id',
    user_name VARCHAR(255) NOT NULL COMMENT 'the account display name at the time of the action',
    role_id BINARY(16) COMMENT 'role Id',
    role_name VARCHAR(255) COMMENT 'role name, eg: NIKA, ADMIN'
);

alter table `character` add column creation_record_id BIGINT;
alter table `character` ADD CONSTRAINT fk_character_creation_record_id__action_lineage_id
    FOREIGN KEY(creation_record_id) REFERENCES action_lineage(id);
alter table `fruit` add column creation_record_id BIGINT;
alter table `fruit` ADD CONSTRAINT fk_fruit_creation_record_id__action_lineage_id
    FOREIGN KEY(creation_record_id) REFERENCES action_lineage(id);
alter table `battle` add column creation_record_id BIGINT;
alter table `battle` ADD CONSTRAINT fk_battle_creation_record_id__action_lineage_id
    FOREIGN KEY(creation_record_id) REFERENCES action_lineage(id);
alter table `battle` drop column action_timestamp;
