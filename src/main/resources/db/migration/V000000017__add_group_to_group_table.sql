alter table `group` add column father_group_id binary(16);
ALTER TABLE `group` ADD CONSTRAINT `fk_group__father_group_id`
    FOREIGN KEY(`father_group_id`) REFERENCES `group`(`id`);