alter table fruit drop foreign key fk_fruit__owner_id;
alter table fruit drop column  owner_id;
alter table `character` drop column fruits;
create table character_to_fruit (
    id binary(16) primary key,
    character_id binary(16) not null ,
    fruit_id binary(16) not null ,
    wake_up boolean not null
);

alter table character_to_fruit add constraint fk_character_id_to_character_id
foreign key character_to_fruit(character_id) references `character`(id);
alter table character_to_fruit add constraint fk_fruit_id_to_fruit_id
    foreign key character_to_fruit(fruit_id) references `fruit`(id);

