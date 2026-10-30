package com.yichi.onepiece.assistant.importing;

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

/** Reads public Baike infoboxes only. No scripts, anti-bot bypass or arbitrary URL fetching. */
@Component
public class BaiduCharacterFacts implements CharacterFacts {
  private static final int MAX_BYTES = 2_000_000;
  private static final Duration TIMEOUT = Duration.ofSeconds(6);
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
      String html = transport.get(URI.create("https://baike.baidu.com/item/" + encoded));
      return html == null ? Map.of() : parse(html, name);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (IOException | RuntimeException e) {
      /* Source unavailable means unknown, never a failed draft. */
    }
    return Map.of();
  }

  static Map<String, String> parse(String html, I18nText name) {
    var page = Jsoup.parse(html);
    page.select("script, style, sup").remove();
    String text = page.text();
    if (!(text.contains("航海王")
        || text.contains("海贼王")
        || text.toLowerCase(Locale.ROOT).contains("one piece"))) return Map.of();
    String expected = BangumiCharacterFacts.normalize(name.getZhCN());
    boolean identity =
        page.select("h1").stream()
            .anyMatch(h -> BangumiCharacterFacts.normalize(h.text()).equals(expected));
    if (!identity) return Map.of();
    Map<String, String> facts = new LinkedHashMap<>();
    Set<String> conflicts = new HashSet<>();
    for (var label :
        page.select(
            ".basic-info dt, [class*=basicInfo] dt, [class*=basicInfoItem] [class*=itemName]")) {
      String key = LABELS.get(label.text().replaceAll("[\\s\\u00a0：:]", ""));
      var value = label.nextElementSibling();
      if (key == null
          || value == null
          || !(value.tagName().equals("dd") || value.className().contains("itemValue"))) continue;
      String fact = value.text().trim();
      if (fact.isEmpty() || fact.length() > 200 || conflicts.contains(key)) continue;
      String old = facts.putIfAbsent(key, fact);
      if (old != null && !old.equals(fact)) {
        facts.remove(key);
        conflicts.add(key);
      }
    }
    return facts;
  }
}
