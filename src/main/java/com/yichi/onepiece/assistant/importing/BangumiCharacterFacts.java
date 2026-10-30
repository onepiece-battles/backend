package com.yichi.onepiece.assistant.importing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.model.I18nText;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Birthdays, heights and ages from Bangumi (bgm.tv), an anime database mainland China can reach:
 * facts the model often misremembers. A search hit counts only when Bangumi lists one of the
 * drafted names (its Simplified Chinese name or an alias such as the English one) and the
 * character appears in One Piece, as for the import's avatars. Any failure means no facts.
 *
 * <p>The hit's own names come along as {@code canonical_zh_CN} / {@code canonical_en_US}: a
 * drafted alias such as 假路飞 finds 丹马罗·布拉克 (Demaro Black), the character's real name.
 */
@Component
public class BangumiCharacterFacts implements CharacterFacts {
  private static final Logger logger = LoggerFactory.getLogger(BangumiCharacterFacts.class);
  private static final String API = "https://api.bgm.tv";
  // Bangumi asks API clients to say who they are.
  private static final String USER_AGENT = "onepiece-battles/1.0 (https://onepiece-battles.com)";
  static final Set<Integer> ONE_PIECE = Set.of(975, 3510);
  static final List<String> FIELDS = List.of("生日", "身高", "年龄");
  private static final Duration TIMEOUT = Duration.ofSeconds(6);

  /** One API call: the response body, or null when it failed. */
  @FunctionalInterface
  interface Transport {
    String call(String method, String path, String body) throws IOException, InterruptedException;
  }

  private final Transport transport;
  private final ObjectMapper json = new ObjectMapper();

  public BangumiCharacterFacts() {
    this(http());
  }

  BangumiCharacterFacts(Transport transport) {
    this.transport = transport;
  }

  private static Transport http() {
    var client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    return (method, path, body) -> {
      var request =
          HttpRequest.newBuilder(URI.create(API + path))
              .timeout(TIMEOUT)
              .header("User-Agent", USER_AGENT)
              .header("Content-Type", "application/json")
              .method(
                  method,
                  body == null
                      ? HttpRequest.BodyPublishers.noBody()
                      : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
              .build();
      var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      return response.statusCode() == 200 ? response.body() : null;
    };
  }

  @Override
  public Map<String, String> find(I18nText name) {
    var drafted = new ArrayList<String>();
    for (var text : new String[] {name.getZhCN(), name.getEnUS()})
      if (text != null && !text.isBlank()) drafted.add(text.trim());
    var wanted = new HashSet<String>();
    drafted.forEach(n -> wanted.add(normalize(n)));
    try {
      for (var keyword : drafted) {
        for (var candidate : search(keyword)) {
          if (Collections.disjoint(names(candidate), wanted)) continue;
          if (inOnePiece(candidate.path("id").asText())) return facts(candidate);
        }
      }
    } catch (IOException | RuntimeException e) {
      logger.debug("no Bangumi facts for {}", drafted, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return Map.of();
  }

  private List<JsonNode> search(String keyword) throws IOException, InterruptedException {
    String body =
        transport.call(
            "POST",
            "/v0/search/characters?limit=5",
            json.createObjectNode().put("keyword", keyword).toString());
    var result = new ArrayList<JsonNode>();
    if (body != null) json.readTree(body).path("data").forEach(result::add);
    return result;
  }

  private boolean inOnePiece(String id) throws IOException, InterruptedException {
    if (!id.matches("\\d+")) return false;
    String body = transport.call("GET", "/v0/characters/" + id + "/subjects", null);
    if (body == null) return false;
    for (var subject : json.readTree(body))
      if (ONE_PIECE.contains(subject.path("id").asInt())) return true;
    return false;
  }

  /** The original name, the Simplified Chinese name and every alias. */
  private static Set<String> names(JsonNode candidate) {
    var names = new HashSet<String>();
    names.add(normalize(candidate.path("name").asText()));
    for (var field : candidate.path("infobox")) {
      var value = field.path("value");
      if (field.path("key").asText().equals("简体中文名") && value.isTextual())
        names.add(normalize(value.asText()));
      if (field.path("key").asText().equals("别名"))
        for (var alias : value) names.add(normalize(alias.path("v").asText()));
    }
    names.remove("");
    return names;
  }

  private static Map<String, String> facts(JsonNode candidate) {
    var facts = new LinkedHashMap<String, String>();
    for (var field : candidate.path("infobox")) {
      String key = field.path("key").asText();
      var value = field.path("value");
      if (key.equals("简体中文名") && value.isTextual() && !value.asText().isBlank())
        facts.put("canonical_zh_CN", value.asText().trim());
      if (key.equals("英文名") && value.isTextual() && !value.asText().isBlank())
        facts.put("canonical_en_US", value.asText().trim());
      if (key.equals("别名"))
        for (var alias : value)
          if (alias.path("k").asText().equals("英文名") && !alias.path("v").asText().isBlank())
            facts.putIfAbsent("canonical_en_US", alias.path("v").asText().trim());
      if (!FIELDS.contains(key)) continue;
      var parts = new ArrayList<String>();
      if (value.isTextual()) parts.add(value.asText());
      for (var part : value) if (part.path("v").isTextual()) parts.add(part.path("v").asText());
      if (!parts.isEmpty()) facts.put(key, String.join("、", parts));
    }
    return facts;
  }

  static String normalize(String name) {
    return name.toLowerCase(Locale.ROOT).replaceAll("[\\s·・･.．\\-_]", "");
  }
}
