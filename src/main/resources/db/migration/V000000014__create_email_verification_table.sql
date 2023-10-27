create table email_verification (
    id binary(16) primary key,
    user_id binary(16) not null,
    email varchar(255) not null,
    email_verification_code varchar(255) not null,
    is_used boolean not null,
    creation_timestamp bigint not null,
    expiration_timestamp bigint not null
);
alter table email_verification add constraint fk_email_verification_user_id__user_id
    foreign key(user_id) references user(id);
