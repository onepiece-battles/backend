create table email_verification (
    id binary(16) primary key,
    user_name varchar(255),
    email varchar(255),
    email_verification_code varchar(255) not null,
    is_used boolean not null,
    creation_timestamp bigint not null,
    expiration_timestamp bigint not null
);
alter table email_verification add constraint fk_email_verification_creation_user_name__user_name
    foreign key(user_name) references user(name);
