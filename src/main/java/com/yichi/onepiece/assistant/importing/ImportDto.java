package com.yichi.onepiece.assistant.importing;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

/**
 * Wire types of the AI batch import. Like the other assistant endpoints these are kept out of the
 * OpenAPI spec. {@code data} holds the same snake_case create fields the assistant's
 * create_character / create_fruit / create_group actions accept ({@code ActionSchemas.fields}).
 */
public final class ImportDto {
  private ImportDto() {}

  public static final String CHARACTER = "character";
  public static final String FRUIT = "fruit";
  public static final String GROUP = "group";

  public record DraftRequest(String scope, String language) {}

  /**
   * One resource of a draft. {@code key} is unique within the draft and is how characters refer to
   * the fruits they eat and the group they belong to. When {@code existingId} is set a resource
   * with exactly this name already exists: it is reused for relations instead of being created,
   * and {@code data} only carries its name.
   */
  public record ImportItem(
      String key,
      String type,
      ObjectNode data,
      String existingId,
      List<String> fruits,
      String group,
      String position,
      List<String> uncertain,
      List<String> problems) {}

  public record ImportDraft(List<ImportItem> items, List<String> notes) {}

  /** One message of the review conversation: {@code role} is "user" or "assistant". */
  public record Turn(String role, String content) {}

  /**
   * The reviewer asks the AI to change the current draft ({@code items}, with the reviewer's own
   * edits). {@code messages} is the conversation so far, oldest first.
   */
  public record ReviseRequest(
      List<ImportItem> items, List<Turn> messages, String instruction, String language) {}

  /**
   * The draft after a revision. {@code reply} answers the reviewer; {@code changed} lists the keys
   * of the items that were added or modified; {@code refreshAvatars} the characters whose portrait
   * the reviewer asked to look up again (a rename alone never does).
   */
  public record ImportRevision(
      List<ImportItem> items,
      List<String> notes,
      String reply,
      List<String> changed,
      List<String> refreshAvatars) {
    public ImportRevision(
        List<ImportItem> items, List<String> notes, String reply, List<String> changed) {
      this(items, notes, reply, changed, List.of());
    }
  }

  public record CommitRequest(List<ImportItem> items, String language) {}

  public record CreatedResource(String key, String type, String id, String name) {}

  public record CommitResult(
      List<CreatedResource> created, int reused, int fruitsEaten, int groupsJoined) {}

  /** A reason an item cannot be imported; {@code key} is null for a problem of the whole batch. */
  public record Problem(String key, String message) {}

  public record CommitRejected(String code, String message, List<Problem> problems) {}
}
