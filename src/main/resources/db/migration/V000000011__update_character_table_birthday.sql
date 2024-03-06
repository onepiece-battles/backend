update `character` set date_of_birth = null where true;
alter table `character` modify column date_of_birth varchar(10);

