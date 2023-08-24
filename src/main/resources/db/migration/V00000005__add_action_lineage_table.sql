create table action_lineage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_timestamp BIGINT NOT NULL COMMENT 'the action timestamp in milliseconds',
    user_id BINARY(16) NOT NULL COMMENT 'either a user or a service account id',
    user_name VARCHAR(255) NOT NULL COMMENT 'the account display name at the time of the action',
    role_id BINARY(16) COMMENT 'role Id',
    role_name VARCHAR(255) COMMENT 'role name, eg: NIKA, ADMIN'
);

