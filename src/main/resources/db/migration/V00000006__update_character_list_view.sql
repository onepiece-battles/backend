CREATE OR REPLACE VIEW `view_character_list_view`
AS
SELECT `character`.*,
       `creation_record`.action_timestamp as creation_timestamp,
       `modification_record`.action_timestamp as modification_timestamp,
       `creation_record`.user_name as creator,
       `creation_record`.role_name,
       `creation_record`.role_id
FROM `character`
         JOIN `action_lineage` as creation_record on creation_record.id = `character`.creation_record_id
         JOIN `action_lineage` as modification_record on modification_record.id = `character`.modification_record_id
         LEFT JOIN `character_type` on `character`.`id` = `character_type`.`character_id`
         LEFT JOIN `haki` on `character`.id = haki.character_id;