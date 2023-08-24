alter table `character` add column creation_record_id BIGINT NOT NULL;
alter table `character` add column modification_record_id BIGINT NOT NULL;

alter table `fruit` add column creation_record_id BIGINT NOT NULL;
alter table `fruit` add column modification_record_id BIGINT NOT NULL;

alter table `battle` add column creation_record_id BIGINT NOT NULL;
alter table `battle` add column modification_record_id BIGINT NOT NULL;

alter table `character` ADD CONSTRAINT fk_character_creation_record_id__action_lineage_id
    FOREIGN KEY(creation_record_id) REFERENCES action_lineage(id);
alter table `fruit` ADD CONSTRAINT fk_fruit_creation_record_id__action_lineage_id
    FOREIGN KEY(creation_record_id) REFERENCES action_lineage(id);
alter table `battle` ADD CONSTRAINT fk_battle_creation_record_id__action_lineage_id
    FOREIGN KEY(creation_record_id) REFERENCES action_lineage(id);

alter table `character` ADD CONSTRAINT fk_character_modification_record_id__action_lineage_id
    FOREIGN KEY(modification_record_id) REFERENCES action_lineage(id);
alter table `fruit` ADD CONSTRAINT fk_fruit_modification_record_id__action_lineage_id
    FOREIGN KEY(modification_record_id) REFERENCES action_lineage(id);
alter table `battle` ADD CONSTRAINT fk_battle_modification_record_id__action_lineage_id
    FOREIGN KEY(modification_record_id) REFERENCES action_lineage(id);

