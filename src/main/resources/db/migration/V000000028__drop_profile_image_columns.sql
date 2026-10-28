-- Profile images are now stored only in OSS, referenced by image_uri.
-- The legacy profile_image BLOB columns are dropped to reclaim storage and heap.
-- PRECONDITION: the image to OSS backfill (SupportService.updateUri) must have
-- been run for USER, GROUP, CHARACTER and FRUIT so every row has an image_uri
-- before this migration is applied, otherwise those rows lose their image.
ALTER TABLE `user` DROP COLUMN `profile_image`;
ALTER TABLE `group` DROP COLUMN `profile_image`;
ALTER TABLE `character` DROP COLUMN `profile_image`;
ALTER TABLE `fruit` DROP COLUMN `profile_image`;
