package com.yichi.onepiece.assistant.importing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

/** Conservative conversions of single, explicit biography facts; ranges/timelines stay unknown. */
final class CharacterFactValues {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Map<String, String> LABELS =
      Map.of(
          "bounty",
          "悬赏金",
          "age",
          "年龄",
          "height",
          "身高",
          "birth_place",
          "出生地",
          "date_of_birth",
          "生日");

  private CharacterFactValues() {}

  static Map<String, JsonNode> read(Map<String, String> facts) {
    Map<String, JsonNode> result = new LinkedHashMap<>();
    LABELS.forEach(
        (field, label) -> {
          String raw = facts.getOrDefault(field, facts.get(label));
          if (raw == null) return;
          String value = raw.replaceAll("\\[\\d+(?:-\\d+)?]", "").replaceAll("[\\s,，]", "").trim();
          if (value.isEmpty()
              || Pattern.compile("不明|未知|未公开|不详|推测|约|大概|可能|unknown|暂无|疑", Pattern.CASE_INSENSITIVE)
                  .matcher(value)
                  .find()) return;
          JsonNode node = null;
          switch (field) {
            case "bounty" -> {
              Long bounty = bounty(value);
              if (bounty != null && bounty >= 10 && bounty <= 100_000_000_000L)
                node = JSON.getNodeFactory().numberNode(bounty);
            }
            case "height", "age" -> {
              var matcher =
                  Pattern.compile(
                          field.equals("age") ? "^(\\d{1,6})(?:岁)?$" : "^(\\d{1,6})(?:cm|厘米)?$",
                          Pattern.CASE_INSENSITIVE)
                      .matcher(value);
              if (matcher.matches()) {
                int number = Integer.parseInt(matcher.group(1));
                if (number >= (field.equals("age") ? 0 : 1) && number <= 100000)
                  node = JSON.getNodeFactory().numberNode(number);
              }
            }
            case "date_of_birth" -> {
              var matcher = Pattern.compile("^(\\d{1,2})(?:月|[-/])(\\d{1,2})日?$").matcher(value);
              if (matcher.matches()) {
                int month = Integer.parseInt(matcher.group(1)),
                    day = Integer.parseInt(matcher.group(2));
                if (month >= 1
                    && month <= 12
                    && day >= 1
                    && day <= java.time.Month.of(month).maxLength())
                  node = JSON.getNodeFactory().textNode(month + "月" + day + "日");
              }
            }
            case "birth_place" -> {
              if (value.length() <= 100 && !value.matches(".*[、/→;；].*"))
                node = JSON.getNodeFactory().textNode(value);
            }
            default -> {}
          }
          if (node != null) result.put(field, node);
        });
    return result;
  }

  static Long bounty(String value) {
    var matcher =
        Pattern.compile(
                "^(\\d+(?:\\.\\d+)?)(亿|万)?(?:(\\d+(?:\\.\\d+)?)万)?(?:贝里|贝利|Berries|berries)?$")
            .matcher(value);
    if (!matcher.matches()) return null;
    try {
      BigDecimal amount = new BigDecimal(matcher.group(1));
      if ("亿".equals(matcher.group(2))) amount = amount.multiply(BigDecimal.valueOf(100_000_000));
      if ("万".equals(matcher.group(2))) amount = amount.multiply(BigDecimal.valueOf(10000));
      if (matcher.group(3) != null)
        amount = amount.add(new BigDecimal(matcher.group(3)).multiply(BigDecimal.valueOf(10000)));
      return amount.longValueExact();
    } catch (ArithmeticException e) {
      return null;
    }
  }

  /**
   * A direct patch may use a fact explicitly supplied by the reviewer, never an invented number.
   */
  static boolean given(String instruction, String field, JsonNode value) {
    if (instruction == null) return false;
    if (value.isTextual()) return instruction.contains(value.asText());
    var matcher =
        Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?(?:亿|万)?(?:\\d+(?:\\.\\d+)?万)?")
            .matcher(instruction);
    while (matcher.find()) {
      String token = matcher.group().replace(",", "");
      Long number =
          field.equals("bounty")
              ? bounty(token)
              : token.matches("\\d+") ? Long.valueOf(token) : null;
      if (number != null && value.isNumber() && number == value.asLong()) return true;
    }
    return false;
  }
}
