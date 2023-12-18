CREATE VIEW `view_group_list_view`
AS
SELECT `group`.*,
   `creation_record`.action_timestamp as creation_timestamp,
   `modification_record`.action_timestamp as modification_timestamp,
   `creation_record`.user_name as creator,
   `creation_record`.role_name
FROM `group`
JOIN `action_lineage` as creation_record on creation_record.id = `group`.creation_record_id
JOIN `action_lineage` as modification_record on modification_record.id = `group`.modification_record_id;
