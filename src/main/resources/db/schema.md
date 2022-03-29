# DB GOLDEN SCHEMA

## user

```sql
CREATE TABLE `user`(
    `id` BINARY(16) NOT NULL,
    `user_name` VARCHAR(255) NOT NULL,
    `organization` TEXT NOT NULL
);
```

```sql
ALTER TABLE `user` ADD CONSTRAINT PRIMARY KEY(`id`);
```
