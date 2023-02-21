alter table `character` rename column character_types to character_type;
alter table `character` MODIFY COLUMN character_type VARCHAR(30) NOT NULL;