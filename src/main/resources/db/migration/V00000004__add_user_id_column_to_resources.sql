alter table `character` add column user_id BINARY(16);
ALTER TABLE `character` ADD CONSTRAINT fk_character__user_id
    FOREIGN KEY(user_id) REFERENCES `user`(id);

alter table `fruit` add column user_id BINARY(16);
ALTER TABLE `fruit` ADD CONSTRAINT fk_fruit__user_id
    FOREIGN KEY(user_id) REFERENCES `user`(id);