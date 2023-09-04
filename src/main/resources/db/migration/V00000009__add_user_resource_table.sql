create table user_resource (
    id binary(16) primary key,
    user_id binary(16),
    source_user_id binary(16),
    resource_type varchar(255),
    resource_ids text # json string of resource ids
);

alter table user_resource add constraint fk_user_resource_user_id__user_id
foreign key  (user_id) references user(id);
alter table user_resource add constraint fk_user_resource_source_user_id__user_id
    foreign key  (source_user_id) references user(id);