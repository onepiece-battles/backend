alter table role_resource
add column permissions text not null;
alter table resource
add column permissions text not null;
