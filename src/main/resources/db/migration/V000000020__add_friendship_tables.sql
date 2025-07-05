CREATE TABLE friendship (
    `id` BINARY(16) PRIMARY KEY NOT NULL,
    `friend_left_id` BINARY(16) NOT NULL,
    `friend_right_id` BINARY(16) NOT NULL,
    `friendship_status` VARCHAR(20) NOT NULL,
    `creation_record_id` BIGINT,
    `modification_record_id` BIGINT
);

ALTER TABLE `friendship` ADD CONSTRAINT `fk_friendship__friend_left_id`
    FOREIGN KEY(`friend_left_id`) REFERENCES `user`(`id`);
ALTER TABLE `friendship` ADD CONSTRAINT `fk_friendship__friend_right_id`
    FOREIGN KEY(`friend_right_id`) REFERENCES `user`(`id`);
