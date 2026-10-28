-- AI tools can only prepare changes. A separate authenticated user message confirms them.
CREATE TABLE assistant_action (
    code VARCHAR(10) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    operation VARCHAR(40) NOT NULL,
    payload MEDIUMTEXT NOT NULL,
    summary MEDIUMTEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    result MEDIUMTEXT NULL,
    INDEX idx_assistant_action_actor (user_id, role_id, status, expires_at)
);
