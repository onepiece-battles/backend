# DB GOLDEN SCHEMA

## user

```sql
CREATE TABLE `user`(
    `id` BINARY(16),
    `name` VARCHAR(255),
    `email` VARCHAR(255),
    `encrypted_password` VARCHAR(100),
    `account_condition` VARCHAR(100),
    `profile_image` LONGBLOB
);
```
