create table battle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    character_id_initiator BINARY(16) NOT NULL,
    character_id_receiver BINARY(16) NOT NULL,
    action_timestamp BIGINT NOT NULL COMMENT 'the timestamp when this fight is created'
);
ALTER TABLE battle ADD CONSTRAINT character_id_initiator
    FOREIGN KEY(owner_id) REFERENCES character_book(id);
ALTER TABLE battle ADD CONSTRAINT character_id_receiver
    FOREIGN KEY(owner_id) REFERENCES character_book(id);