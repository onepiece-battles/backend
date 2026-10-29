-- FAILED only ever marked battles that hit the turn limit. They are draws and have no winner.
UPDATE `battle_execution` SET `winner_id` = NULL WHERE `battle_status` = 'FAILED';
