create table token (
    id BINARY(16) NOT NULL PRIMARY KEY,
    token_value VARCHAR(255) NOT NULL,
    token_type VARCHAR(255) NOT NULL,
    revoked TINYINT NOT NULL,
    expired TINYINT NOT NULL,
    user_id binary(16) NOT NULL
);
ALTER TABLE token ADD CONSTRAINT fk_token_user_id__user_id
    FOREIGN KEY(user_id) REFERENCES user(id);
