CREATE OR REPLACE VIEW `view_battle_list_view`
AS
SELECT
    `battle`.*,
    `creation_record`.user_name as creator,
    `creation_record`.role_name,
    `creation_record`.action_timestamp as creation_timestamp,
    `modification_record`.action_timestamp as modification_timestamp,
    `starter`.name as starter_name,
    `starter`.profile_image as starter_image,
    `receiver`.name as receiver_name,
    `receiver`.profile_image as receiver_image,
    `battle_execution`.battle_status as battle_status,
    `battle_execution`.battle_report_stage as battle_report_stage,
    `battle_execution`.winner_id
FROM `battle` JOIN `character` AS `starter` on battle.starter_id = starter.id
              JOIN `character` AS `receiver` on battle.receiver_id = receiver.id
              JOIN `battle_execution` on battle.id = battle_execution.battle_id
              JOIN `action_lineage` as creation_record on creation_record.id = battle.creation_record_id
              JOIN `action_lineage` as modification_record on modification_record.id = battle.modification_record_id;