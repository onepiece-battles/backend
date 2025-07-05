create table notification (
    `id` BINARY(16) PRIMARY KEY NOT NULL,
    `content` text not null,
    `category` varchar(255) not null,
    `notification_source_id` BINARY(16) not null,
    `notification_target_id` BINARY(16) not null,
    `creation_record_id` BIGINT not null,
    `modification_record_id` BIGINT not null,
    `received` boolean not null,
    `system_notification` boolean not null
);
ALTER TABLE `notification` ADD CONSTRAINT `fk_notification__notification_source_id`
    FOREIGN KEY(`notification_source_id`) REFERENCES `user`(`id`);
ALTER TABLE `notification` ADD CONSTRAINT `fk_notification__notification_target_id`
    FOREIGN KEY(`notification_target_id`) REFERENCES `user`(`id`);