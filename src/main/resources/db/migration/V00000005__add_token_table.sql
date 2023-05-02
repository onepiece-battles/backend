create table token(
    id BINARY(16) PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    token_type VARCHAR(255) NOT NULL,
    revoked TINYINT NOT NULL,
    expired TINYINT NOT NULL,
    user_id BINARY(16)
);
ALTER TABLE token ADD CONSTRAINT fk_token__user_id FOREIGN KEY (user_id) REFERENCES user(id);