create table file_object(
    id binary(16) primary key,
    file_name varchar(255) not null,
    resource_id binary(16),
    creator_user_id binary(16),
    creation_record_id bigint not null,
    file_category varchar(255) not null,
    http_mime_type varchar(255) not null,
    oss_uri varchar(255) not null
);
ALTER TABLE `file_object` ADD CONSTRAINT fk_file_creator_user_id__user_id
    FOREIGN KEY(creator_user_id) REFERENCES user(id);
alter table `file_object` ADD CONSTRAINT fk_file_creation_record_id__action_lineage_id
    FOREIGN KEY(creation_record_id) REFERENCES action_lineage(id);