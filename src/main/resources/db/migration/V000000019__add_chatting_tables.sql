CREATE TABLE `conversation`(
    `id` BINARY(16) NOT NULL,
    `group` BOOLEAN NOT NULL, # group chat is the chat when more than 3 people in it
    `private_group` BOOLEAN NOT NULL, # private chat is the chat that can not be search from public
    `creation_record_id` BIGINT,
    `modification_record_id` BIGINT
);
ALTER TABLE `conversation` ADD CONSTRAINT PRIMARY KEY(`id`);

CREATE TABLE IF NOT EXISTS `message`(
                                        `id` BINARY(16) NOT NULL,
                                        `user_id` BINARY(16) NOT NULL, # owner of this phrase
                                        `conversation_id` BINARY(16) NOT NULL,
                                        `pre_message_id` BINARY(16) NOT NULL,
                                        `content` VARCHAR(255),
                                        `retracted` BOOLEAN NOT NULL,
                                        `image_content_uri` VARCHAR(255),
                                        `voice_content_uri` VARCHAR(255),
                                        `creation_record_id` BIGINT,
                                        `modification_record_id` BIGINT
);
ALTER TABLE `message` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `message` ADD CONSTRAINT `fk_message__user_id`
    FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
ALTER TABLE `message` ADD CONSTRAINT `fk_message__pre_message_id`
    FOREIGN KEY(`pre_message_id`) REFERENCES `message`(`id`);
ALTER TABLE `message` ADD CONSTRAINT `fk_message__conversation_id`
    FOREIGN KEY(`conversation_id`) REFERENCES `conversation`(`id`);

CREATE TABLE `user_conversation`(
                                    `id` BINARY(16) NOT NULL,
                                    `user_id` BINARY(16) NOT NULL,
                                    `conversation_id` BINARY(16) NOT NULL
);
ALTER TABLE `user_conversation` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `user_conversation` ADD CONSTRAINT `fk_user_conversation__user_id`
    FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
ALTER TABLE `user_conversation` ADD CONSTRAINT `fk_user_conversation__conversation_id`
    FOREIGN KEY(`conversation_id`) REFERENCES `conversation`(`id`);

