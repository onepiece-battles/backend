# AI resource toolkit

The server owns the tool registry, DeepSeek prompt and execution loop. `/api/onepiece/assistant/ask`
keeps its existing `status`, `delta`, `done` and `error` SSE events; the frontend does not need a
new protocol or an API key.

## Available tools (42)

| Area | Tools |
| --- | --- |
| Existing compatibility tools | `search_characters`, `get_character_detail`, `list_top_characters`, `propose_battle`, `search_fruits`, `get_fruit_holders`, `list_fruits_by_type`, `search_groups`, `get_group_detail` |
| Paged search and counts | `query_characters`, `query_fruits`, `query_groups`, `search_battles`, `count_resources` |
| Character relationships and analysis | `get_character_profile`, `get_character_fruits`, `get_character_groups`, `compare_characters`, `preview_eat_fruit` |
| Fruit and group details | `get_fruit_detail`, `get_group_members`, `get_group_hierarchy` |
| Battle records | `get_battle_detail`, `get_battle_status`, `get_battle_rounds`, `get_battle_report`, `get_character_battles`, `get_head_to_head` |
| Fields and pending changes | `get_resource_action_schema`, `list_pending_actions` |
| Confirmed character changes | `prepare_create_character`, `prepare_update_character`, `prepare_eat_fruit`, `prepare_join_group` |
| Confirmed fruit changes | `prepare_create_fruit`, `prepare_update_fruit` |
| Confirmed group changes | `prepare_create_group`, `prepare_update_group`, `prepare_add_group_members`, `prepare_attach_subgroups` |
| Confirmed battle changes | `prepare_create_battle`, `prepare_start_battle` |

Paged queries return `total_count`, `page`, `page_size` and `has_more`. Page sizes are limited to 30.
`all_visible` includes the caller's own, system and explicitly shared resources; `mine` and `system`
are strict scopes that exclude incidental shares. Combined character filters cover race, battle
style, haki and fruit ownership. Fruit filters cover types; group filters cover category and root
membership; battle filters cover both participants, type and status.

Details accept UUIDs returned by the tools or an unambiguous resource name. Ambiguous names return
candidate links, never the first arbitrary match. Group members use character IDs rather than
membership IDs. Hierarchies stop at 4 levels / 50 nodes and mark truncation. Fruit previews apply
the existing coefficient formula and clamping without touching the database. Battle history covers
both participant orientations, and wins are counted only for finished records with a winner.

## User confirmation

For example, the user says `把路飞的基础攻击改为 500`:

1. `prepare_update_character` resolves the resource, validates the fields, checks IAM and ownership,
   snapshots the current data and stores a `PREPARED` proposal. Resource data remains unchanged.
2. The server appends a reviewable summary, including old/new field values and affected links, to
   the persisted and streamed answer. That proposal then becomes `PENDING`.
3. The user sends **`确认 A1B2C3D4E5`** or **`confirm A1B2C3D4E5`** in a new message. The code shown
   is an example; use the actual code in the proposal. **`取消 CODE`** / **`cancel CODE`** discards it.
4. The server recognizes only the entire confirmation/cancellation command, independently of
   DeepSeek. Quoted text, a model tool call, and a plain `确认` cannot execute a write.
5. Confirmation binds the account and login role, checks the 15-minute expiry, locks the proposal
   and affected resource rows, rechecks permissions/ownership and compares the snapshot. Changed
   data requires a fresh proposal. Execution and the stored result commit in one transaction.
   Snapshot numbers are compared exactly by value, independent of Java/Jackson integer widths
   after JSON persistence. Strings, booleans, missing fields and array ordering remain significant;
   fruit modifier order must not be ignored.
6. A repeated or concurrent confirmation returns the stored result without performing the write
   again. Service failures roll back both the change and confirmation result.

There are at most 8 unexpired unfinished proposals per account/role. Unpublished drafts, expired
codes, other users' codes and codes created under another login role cannot execute. Codes and
results persist through application restarts. Confirmation itself does not call DeepSeek and works
even if its key is subsequently unavailable.

Only NIKA or the creator in the original role can edit an existing resource through these tools,
in addition to normal IAM permissions. IDs, ownership, scores, image URLs and lineage metadata
are not editable AI fields. Localized name patches preserve the other language; character stat
patches change base stats and recalculate effective values. Existing OSS avatars, creation records
and system-resource flags are preserved by the update services.

Joining a crew changes current membership and retains previous membership as history. Moving
subgroups requires ownership of the parent and children and uses the existing hierarchy cycle
checks. Fruit coefficient edits recalculate holder stats. Creating a battle leaves it waiting;
starting it requires a separate confirmation and can update combat scores, generate a report and
send the existing battle notification/report emails. A completed start confirmation means the
battle was submitted to the existing asynchronous scheduler; use status/report tools to check its
outcome. This change does not replace the application's in-memory Quartz job store.

No deletion, arbitrary HTTP/SQL, file upload, account/role administration or automatic multi-step
write execution is exposed. The legacy `propose_battle` frontend confirmation card remains supported
for compatibility, while the prompt directs new write requests to the server-confirmed tools.

## Deployment and verification

Flyway migration `V000000030__assistant_action_confirmation.sql` adds only the `assistant_action`
table. Existing resource APIs and the SSE DTOs remain compatible. Deploy the backend normally;
Flyway creates the table at startup. No frontend build is required for these tools.

Run with JDK 17:

```sh
./gradlew test --tests 'com.yichi.onepiece.assistant.*' --console=plain
./gradlew bootJar --console=plain
```

Tests cover real mapper scope SQL, real confirmation persistence/row locking/rollback/concurrency,
argument allowlists, ambiguous names, relation IDs, bounded hierarchies, read-only fruit previews,
head-to-head results, ownership/stale snapshots and the unchanged streaming contract. The database
harness uses isolated H2 schemas by default; setting `ONEPIECE_TEST_MYSQL_PORT` and
`ONEPIECE_TEST_MYSQL_PASSWORD` runs its database tests against disposable MySQL schemas instead.

Validation note: the existing full-suite `DatabaseSchemaTests` and `MailServiceTests` require a
working application datasource and fail to initialize in this local configuration (database
authentication failure). The AI-tool tests and complete migration test have also been run against
a separate, automatically removed MySQL 8 container without using the application database.

## Confirmation cards

The `done` SSE event and stored assistant messages also expose an optional `actionProposals`
array. Each entry contains the server's code, operation, title, exact summary, status, expiry
and completed-result resource links. Clients render confirmation/cancellation buttons that send
an ordinary explicit `确认 CODE` / `取消 CODE` (or English equivalent) to `/ask`; they must not
create a second resource through the legacy battle API.

The array is projected from the confirmation store under the authenticated account and role.
History restores current pending, completed, cancelled or expired states, including older
text-only proposal answers whose codes can be verified in that store. Unknown or unpublished
codes do not produce cards. Existing clients may continue to use the textual instructions;
the original `battleProposal` field remains compatible with legacy replies.

## Import draft revisions

`/assistant/import/revise` still returns the same item DTOs. A revision is now bounded on the server
by the current instruction's resource names and fields. Earlier turns resolve an unambiguous follow-up;
they do not authorize replaying earlier edits. Unnamed fields and reviewer edits are preserved. Empty
`redraft.fields` no longer means every field: it can only select the fields of the current request.
Explicit optional-field clearing is supported. Negated instructions protect their fields. Ambiguous
requests leave the draft unchanged so the reviewer can name the target/field. Actual item differences,
rather than the model's claim, determine `changed`. Additions keep necessary fruit/holder dependencies
but exclude unsolicited members/groups. Removing a new character also removes a newly drafted fruit
that no remaining character holds.

Biographical values (bounty, age, height, birthplace and birthday) use public Baidu Baike infoboxes plus
Bangumi. Baike must match the drafted Chinese name and identify One Piece. Requests are restricted to
HTTPS on `baike.baidu.com`, with bounded responses, timeouts and no scripts or anti-bot bypass. A 403,
network failure, mismatched identity, ambiguous timeline or conflicting normalized source values means
unknown. Unknown optional facts are omitted, never converted to zero or clamped into a plausible value.
A reviewer may supply an explicit value in conversation or manually in the editor. Required combat
attributes remain estimates calibrated to the app's reference resources, separate from biography facts.
External infobox text is untrusted reference data, never instructions.

The frontend derives a field-by-field before/after review and offers undo of the last AI revision.
Undo preserves the pre-revision selection/avatar state; a later manual edit invalidates that snapshot
so undo cannot discard the reviewer's newer work. Normal create/commit permission checks and the
explicit import action are unchanged. No database migration or new API shape is required.
