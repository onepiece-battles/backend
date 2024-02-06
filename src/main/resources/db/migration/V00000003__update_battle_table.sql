alter table battle add column `system_resource` boolean;
update battle set battle.`system_resource` = true where battle.system_resource is null;
alter table battle modify column `system_resource` boolean not null;