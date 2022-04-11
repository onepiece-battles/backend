alter table user ADD COLUMN encrypted_password VARCHAR(255) NOT NULL;
alter table user ADD COLUMN account_condition VARCHAR(40) NOT NULL;