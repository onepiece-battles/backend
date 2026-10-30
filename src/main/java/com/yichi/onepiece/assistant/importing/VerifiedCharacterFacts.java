package com.yichi.onepiece.assistant.importing;

import com.yichi.onepiece.model.I18nText;
import java.util.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Independent sources fill gaps; conflicting values are deliberately left unknown. The real name
 * is Baike's Chinese and Bangumi's English one, each falling back to the other source.
 */
@Component
@Primary
public class VerifiedCharacterFacts implements CharacterFacts {
  private final BangumiCharacterFacts bangumi;
  private final BaiduCharacterFacts baidu;

  public VerifiedCharacterFacts(BangumiCharacterFacts bangumi, BaiduCharacterFacts baidu) {
    this.bangumi = bangumi;
    this.baidu = baidu;
  }

  @Override
  public Map<String, String> find(I18nText name) {
    var fromBangumi = bangumi.find(name);
    var fromBaidu = baidu.find(name);
    var first = CharacterFactValues.read(fromBangumi);
    var second = CharacterFactValues.read(fromBaidu);
    Map<String, String> result = new LinkedHashMap<>();
    var fields = new HashSet<>(first.keySet());
    fields.addAll(second.keySet());
    for (String field : fields) {
      var a = first.get(field);
      var b = second.get(field);
      if (a != null && b != null && !a.equals(b)) continue;
      result.put(field, (a == null ? b : a).asText());
    }
    putFirst(result, "canonical_zh_CN", fromBaidu, fromBangumi);
    putFirst(result, "canonical_en_US", fromBangumi, fromBaidu);
    return result;
  }

  private static void putFirst(
      Map<String, String> result, String key, Map<String, String> first, Map<String, String> second) {
    for (var source : List.of(first, second)) {
      String value = source.get(key);
      if (value != null && !value.isBlank()) {
        result.put(key, value);
        return;
      }
    }
  }
}
