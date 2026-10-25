DELETE FROM `user_resource`
WHERE `user_id` IS NULL OR `resource_type` IS NULL OR `resource_id` IS NULL;

DELETE FROM `user_resource`
WHERE `id` IN (
    SELECT `duplicate_id` FROM (
        SELECT DISTINCT newer.`id` AS `duplicate_id`
        FROM `user_resource` newer
        INNER JOIN `user_resource` older
            ON newer.`user_id` = older.`user_id`
            AND newer.`resource_type` = older.`resource_type`
            AND newer.`resource_id` = older.`resource_id`
            AND newer.`id` > older.`id`
    ) duplicates
);

ALTER TABLE `user_resource` MODIFY COLUMN `user_id` BINARY(16) NOT NULL;
ALTER TABLE `user_resource` MODIFY COLUMN `resource_type` VARCHAR(255) NOT NULL;
ALTER TABLE `user_resource` MODIFY COLUMN `resource_id` BINARY(16) NOT NULL;
ALTER TABLE `user_resource`
    ADD CONSTRAINT `uk_user_resource__user_type_resource`
    UNIQUE (`user_id`, `resource_type`, `resource_id`);
