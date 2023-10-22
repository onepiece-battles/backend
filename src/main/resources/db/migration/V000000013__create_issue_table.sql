create table issue (
    id binary(16) not null primary key,
    creation_user_id binary(16) not null,
    issue_type varchar(255) not null, # [feature suggestion, bug report, other]
    issue_header varchar(255) not null,
    issue_description text,
    issue_status varchar(255) not null, # [created, in_progress, resolved, backlogged, revoked]
    creation_timestamp bigint not null,
    modification_timestamp bigint not null
);

alter table issue add constraint fk_issue_creation_user_id__user_id
    foreign key(creation_user_id) references user(id);