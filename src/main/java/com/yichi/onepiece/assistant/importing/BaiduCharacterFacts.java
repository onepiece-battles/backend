package com.yichi.onepiece.assistant.importing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.model.I18nText;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/**
 * Reads the infobox ("card") of a Baidu Baike entry through Baike's public lemma card API, which
 * answers JSON (the entry pages themselves answer a human check). Only an entry whose title is
 * the character's name and which belongs to One Piece counts. No scripts, anti-bot bypass or
 * arbitrary URL fetching.
 */
@Component
public class BaiduCharacterFacts implements CharacterFacts {
  private static final int MAX_BYTES = 2_000_000;
  private static final Duration TIMEOUT = Duration.ofSeconds(6);
  private static final String CARD_API =
      "https://baike.baidu.com/api/openapi/BaikeLemmaCardApi?scope=103&format=json&appid=379020"
          + "&bk_length=600&bk_key=";
  private static final Map<String, String> LABELS =
      Map.ofEntries(
          Map.entry("悬赏金", "悬赏金"),
          Map.entry("悬赏", "悬赏金"),
          Map.entry("赏金", "悬赏金"),
          Map.entry("年龄", "年龄"),
          Map.entry("身高", "身高"),
          Map.entry("生日", "生日"),
          Map.entry("出生日期", "生日"),
          Map.entry("出生地", "出生地"),
          Map.entry("出生地点", "出生地"));
  private static final ObjectMapper JSON = new ObjectMapper();

  @FunctionalInterface
  interface Transport {
    String get(URI uri) throws IOException, InterruptedException;
  }

  private final Transport transport;

  public BaiduCharacterFacts() {
    this(http());
  }

  BaiduCharacterFacts(Transport transport) {
    this.transport = transport;
  }

  private static Transport http() {
    var client =
        HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    return uri -> {
      // Redirects are followed only on the same trusted HTTPS source (ordinary canonicalization).
      for (int redirects = 0; redirects < 4; redirects++) {
        if (!"https".equals(uri.getScheme())
            || !"baike.baidu.com".equals(uri.getHost())
            || uri.getPort() != -1) return null;
        var request =
            HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("User-Agent", "onepiece-battles/1.0 (https://onepiece-battles.com)")
                .header("Accept", "application/json")
                .GET()
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (var body = response.body()) {
          int status = response.statusCode();
          if (Set.of(301, 302, 303, 307, 308).contains(status)) {
            var location = response.headers().firstValue("Location");
            if (location.isEmpty()) return null;
            uri = uri.resolve(location.get());
            continue;
          }
          if (status != 200) return null;
          byte[] bytes = body.readNBytes(MAX_BYTES + 1);
          return bytes.length > MAX_BYTES ? null : new String(bytes, StandardCharsets.UTF_8);
        }
      }
      return null;
    };
  }

  @Override
  public Map<String, String> find(I18nText name) {
    if (name == null || name.getZhCN() == null || name.getZhCN().isBlank()) return Map.of();
    try {
      String encoded =
          URLEncoder.encode(name.getZhCN().trim(), StandardCharsets.UTF_8).replace("+", "%20");
      String card = transport.get(URI.create(CARD_API + encoded));
      return card == null ? Map.of() : parse(card, name);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (IOException | RuntimeException e) {
      /* Source unavailable means unknown, never a failed draft. */
    }
    return Map.of();
  }

  /**
   * The card's facts by label, as single plain values. The entry must be One Piece's and be the
   * character's: its title or Chinese name is the drafted name, or one of its foreign names is
   * the drafted English name. A label with differing values is left out.
   */
  static Map<String, String> parse(String card, I18nText name) throws IOException {
    JsonNode root = JSON.readTree(card);
    if (root == null || !root.isObject()) return Map.of();
    var entries = new HashMap<String, List<String>>();
    for (var entry : root.path("card")) {
      var values = new ArrayList<String>();
      entry.path("value").forEach(v -> values.add(plain(v.asText())));
      entries.computeIfAbsent(entry.path("name").asText(), k -> new ArrayList<>()).addAll(values);
    }
    String series =
        (root.path("desc").asText() + " " + entries.getOrDefault("登场作品", List.of()))
            .toLowerCase(Locale.ROOT);
    if (!(series.contains("航海王") || series.contains("海贼王") || series.contains("one piece")))
      return Map.of();
    String zh = BangumiCharacterFacts.normalize(name.getZhCN());
    String en = name.getEnUS() == null ? "" : BangumiCharacterFacts.normalize(name.getEnUS());
    var titles = new ArrayList<>(entries.getOrDefault("中文名", List.of()));
    titles.add(root.path("title").asText());
    boolean identity =
        titles.stream().anyMatch(t -> BangumiCharacterFacts.normalize(t).equals(zh))
            || !en.isEmpty()
                && entries.getOrDefault("外文名", List.of()).stream()
                    .anyMatch(t -> BangumiCharacterFacts.normalize(t).equals(en));
    if (!identity) return Map.of();

    Map<String, String> facts = new LinkedHashMap<>();
    Set<String> conflicts = new HashSet<>();
    entries.forEach(
        (label, values) -> {
          String key = LABELS.get(label.replaceAll("[\\s\\u00a0：:]", ""));
          if (key == null) return;
          for (String raw : values) {
            String fact = latest(raw);
            if (fact.isEmpty() || fact.length() > 200 || conflicts.contains(key)) continue;
            String old = facts.putIfAbsent(key, fact);
            if (old != null && !old.equals(fact)) {
              facts.remove(key);
              conflicts.add(key);
            }
          }
        });
    return facts;
  }

  /** A card value as plain text: without reference marks, links or markup. */
  static String plain(String html) {
    return Jsoup.parse(html.replaceAll("(?s)<sup[^>]*>.*?</sup>", "")).text().trim();
  }

  /**
   * The current value of a timeline like "17岁（出海）→19岁（进入新世界）": remarks in brackets
   * dropped, the last step kept. Anything else (lists, ranges, "约…") stays as it is for {@link
   * CharacterFactValues} to accept or reject.
   */
  static String latest(String value) {
    String bare = value.replaceAll("\\([^()]*\\)|（[^（）]*）", "").trim();
    return bare.substring(bare.lastIndexOf('→') + 1).trim();
  }
}
