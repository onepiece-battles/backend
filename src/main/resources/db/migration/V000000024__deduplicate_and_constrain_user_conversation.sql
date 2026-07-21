# Prior to this migration, joinChat/createUserConversation could be called
# repeatedly for the same (user_id, conversation_id) pair with no existing-row
# check, producing duplicate membership rows. Those duplicates made
# listChat/initializeSession replay the same conversation and its message
# history more than once to the client. Deduplicate existing data (keep the
# lowest id per pair) before adding the constraint that prevents recurrence.
DELETE uc1 FROM `user_conversation` uc1
    INNER JOIN `user_conversation` uc2
        ON uc1.`user_id` = uc2.`user_id`
        AND uc1.`conversation_id` = uc2.`conversation_id`
        AND uc1.`id` > uc2.`id`;

ALTER TABLE `user_conversation`
    ADD CONSTRAINT `uk_user_conversation__user_id__conversation_id`
    UNIQUE (`user_id`, `conversation_id`);
