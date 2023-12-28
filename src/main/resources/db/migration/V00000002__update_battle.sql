alter table `battle` add column report_file_id binary(16);
ALTER TABLE `battle` ADD CONSTRAINT `fk_battle__report_file_id`
    FOREIGN KEY(`report_file_id`) REFERENCES `file_object`(`id`);