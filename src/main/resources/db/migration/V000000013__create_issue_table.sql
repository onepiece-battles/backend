create table issue (
    id bigint(16) not null primary key,
    issue_type varchar(255) not null, # [feature suggestion, bug report, other]
    issue_header varchar(255) not null,
    issue_description text,
    issue_status varchar(255) not null, # [created, in_progress, resolved, backlogged, revoked]
    creation_timestamp bigint not null,
    modification_timestamp bigint not null
);