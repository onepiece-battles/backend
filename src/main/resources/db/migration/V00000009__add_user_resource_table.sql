create table user_resource (
    id binary(16) primary key,
    user_id binary(16),
    resource_type varchar(255),
    resource_id binary(16)
);

alter table user_resource add constraint fk_user_resource_user_id__user_id
foreign key  (user_id) references user(id);