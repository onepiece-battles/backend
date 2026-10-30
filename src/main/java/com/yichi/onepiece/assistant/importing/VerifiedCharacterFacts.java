package com.yichi.onepiece.assistant.importing;

import com.yichi.onepiece.model.I18nText;
import java.util.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Independent sources fill gaps; conflicting values are deliberately left unknown. */
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
    var first = CharacterFactValues.read(bangumi.find(name));
    var second = CharacterFactValues.read(baidu.find(name));
    Map<String, String> result = new LinkedHashMap<>();
    var fields = new HashSet<>(first.keySet());
    fields.addAll(second.keySet());
    for (String field : fields) {
      var a = first.get(field);
      var b = second.get(field);
      if (a != null && b != null && !a.equals(b)) continue;
      result.put(field, (a == null ? b : a).asText());
    }
    return result;
  }
}
