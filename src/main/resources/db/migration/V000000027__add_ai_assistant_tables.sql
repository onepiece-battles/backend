# AI assistant: server-held DeepSeek settings (single row, Nika-configurable)
# plus per-user conversation history, mirroring the chatting tables.

CREATE TABLE `ai_setting`(
    `id` BINARY(16) NOT NULL,
    `api_key` TEXT,                 # DeepSeek API key, server-side only, never returned to clients
    `base_url` VARCHAR(255),        # chat-completions endpoint, defaulted in code when null
    `model` VARCHAR(64),            # overrides the request model when set
    `creation_record_id` BIGINT,
    `modification_record_id` BIGINT
);
ALTER TABLE `ai_setting` ADD CONSTRAINT PRIMARY KEY(`id`);

CREATE TABLE `ai_conversation`(
    `id` BINARY(16) NOT NULL,
    `user_id` BINARY(16) NOT NULL,  # one running AI thread per user
    `creation_record_id` BIGINT,
    `modification_record_id` BIGINT
);
ALTER TABLE `ai_conversation` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `ai_conversation` ADD CONSTRAINT `fk_ai_conversation__user_id`
    FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);

CREATE TABLE `ai_message`(
    `id` BINARY(16) NOT NULL,
    `conversation_id` BINARY(16) NOT NULL,
    `user_id` BINARY(16) NOT NULL,  # owner of the thread (sender for user turns)
    `role` VARCHAR(16) NOT NULL,    # user | assistant
    `content` TEXT,                 # markdown for assistant turns, plain text for user turns
    `resources` TEXT,               # JSON array of {type,id,name} links referenced in the answer
    `creation_record_id` BIGINT,
    `modification_record_id` BIGINT
);
ALTER TABLE `ai_message` ADD CONSTRAINT PRIMARY KEY(`id`);
ALTER TABLE `ai_message` ADD CONSTRAINT `fk_ai_message__conversation_id`
    FOREIGN KEY(`conversation_id`) REFERENCES `ai_conversation`(`id`);
ALTER TABLE `ai_message` ADD CONSTRAINT `fk_ai_message__user_id`
    FOREIGN KEY(`user_id`) REFERENCES `user`(`id`);
