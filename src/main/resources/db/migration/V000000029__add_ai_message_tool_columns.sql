-- The assistant agent now runs on the server and keeps the tool-calling rounds
-- of each conversation, so follow-up questions can reuse earlier lookups.
-- An assistant row with tool_calls holds the JSON array of calls the model
-- made; each result is a role = tool row keyed by tool_call_id. Neither kind
-- is shown in the chat list.
ALTER TABLE `ai_message` ADD COLUMN `tool_calls` TEXT;
ALTER TABLE `ai_message` ADD COLUMN `tool_call_id` VARCHAR(64);
