alter table user_conversation add column last_read_message_id binary(16);
ALTER TABLE `user_conversation` ADD CONSTRAINT fk_user_conversation__last_read_message_id FOREIGN KEY(last_read_message_id) REFERENCES `message`(`id`);

