package com.yichi.infra.support.entity;

import com.google.common.collect.ImmutableMap.Builder;
import com.yichi.infra.support.entity.EntityProcessor.EntityClass;
import com.yichi.infra.support.entity.EntityProcessor.EntityField;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;

public class EntityGenerator extends BaseCodeGenerator {
  public static final Map<String, String> DefaultValues =
      (new Builder())
          .put("java.lang.String", "\"\"")
          .put("java.lang.Byte", "0")
          .put("java.lang.Character", "0")
          .put("java.lang.Short", "0")
          .put("java.lang.Integer", "0")
          .put("java.lang.Float", "0F")
          .put("java.lang.Long", "0L")
          .put("java.lang.Double", "0D")
          .put("java.lang.Boolean", "false")
          .put("java.util.List", "com.google.common.collect.ImmutableList.of()")
          .put("java.util.Map", "com.google.common.collect.ImmutableMap.of()")
          .put("java.util.Set", "com.google.common.collect.ImmutableSet.of()")
          .put("java.util.Optional", "java.util.Optional.empty()")
          .build();

  EntityGenerator(ProcessingEnvironment processingEnvironment, Writer writer) {
    super(processingEnvironment, writer);
  }

  public String getDefaultValue(EntityClass entity, EntityField field) {
    String defaultValue = field.requiredAnnotation.defaultValue.trim();
    String errorMessage;
    if (defaultValue.isEmpty()) {
      if (DefaultValues.containsKey(field.typeClassName)) {
        return (String) DefaultValues.get(field.typeClassName);
      } else {
        errorMessage =
            String.format(
                "%s.%s has @Required annotation without default value, we do not know how to"
                    + " generate it,  see %s for further details.",
                entity.name,
                field.fieldName,
                "https://yaocheng.feishu.cn/docs/doccnVYlgEtqzGvfoO4UxCRn4ug#x3qWCR");
        System.err.println(errorMessage);
        throw new RuntimeException(errorMessage);
      }
    } else if ("null".equals(defaultValue)) {
      errorMessage =
          String.format(
              "%s.%s has @Required annotation but the defaultValue is set as null, see %s for"
                  + " further details.",
              entity.name,
              field.fieldName,
              "https://yaocheng.feishu.cn/docs/doccnVYlgEtqzGvfoO4UxCRn4ug#x3qWCR");
      System.err.println(errorMessage);
      throw new RuntimeException(errorMessage);
    } else {
      return defaultValue;
    }
  }

  public String toParameters(List<EntityField> fields) {
    StringBuilder sb = new StringBuilder();
    boolean started = false;

    EntityField field;
    for (Iterator var4 = fields.iterator(); var4.hasNext(); sb.append(field.getter).append("()")) {
      field = (EntityField) var4.next();
      if (started) {
        sb.append(", ");
      } else {
        started = true;
      }
    }

    return sb.toString();
  }

  public void generatePackage(EntityClass entity) {
    if (entity.packageName != null && !entity.packageName.isEmpty()) {
      this.println("package %s;\n", new Object[] {entity.packageName});
    }
  }

  public void generateDefaultConstructor(String type, String className) {
    this.println();
    this.println("  %s %s() {", new Object[] {type, className});
    this.println("  }");
  }

  public void generateAllArgsConstructor(String type, String className, List<EntityField> fields) {
    if (!fields.isEmpty()) {
      this.println();
      this.print("  %s %s(", new Object[] {type, className});
      boolean started = false;

      Iterator var5;
      EntityField field;
      for (var5 = fields.iterator();
          var5.hasNext();
          this.print("%s %s", new Object[] {field.typeName, field.fieldName})) {
        field = (EntityField) var5.next();
        if (started) {
          this.print(", ");
        } else {
          started = true;
        }
      }

      this.println(") {");
      var5 = fields.iterator();

      while (var5.hasNext()) {
        field = (EntityField) var5.next();
        this.println("    %s(%s);", new Object[] {field.setter, field.fieldName});
      }

      this.println("  }");
    }
  }

  public void generateMembers(EntityClass entity) {
    if (!entity.fields.isEmpty()) {
      this.println();
      Iterator var2 = entity.fields.iterator();

      while (var2.hasNext()) {
        EntityField field = (EntityField) var2.next();
        this.println("  private %s %s;", new Object[] {field.typeName, field.fieldName});
      }
    }
  }

  public void generateGetter(EntityField field) {
    String type = field.typeName;
    String name = field.fieldName;
    this.println();
    this.println("  @Override");
    this.println("  public %s %s() {", new Object[] {type, field.getter});
    this.println("    return %s;", new Object[] {name});
    this.println("  }");
  }

  private String toParametersForHashCode(List<EntityField> fields) {
    StringBuilder sb = new StringBuilder();
    boolean started = false;
    Iterator var4 = fields.iterator();

    while (var4.hasNext()) {
      EntityField field = (EntityField) var4.next();
      if (started) {
        sb.append(", ");
      } else {
        started = true;
      }

      if (field.isArray) {
        sb.append("java.util.Arrays.hashCode(").append(field.getter).append("())");
      } else {
        sb.append(field.getter).append("()");
      }
    }

    return sb.toString();
  }

  public void generateEqualsAndHashCodeAndToString(EntityClass entity, String typeName) {
    boolean hasDeclareMember = !entity.fields.isEmpty();
    this.println();
    this.println("  @Override");
    this.println("  public int hashCode() {");
    this.println(
        "    return java.util.Objects.hash(%s);",
        new Object[] {this.toParametersForHashCode(entity.fields)});
    this.println("  }");
    this.println();
    this.println("  @Override");
    this.println("  public boolean equals(java.lang.Object o) {");
    this.println("    if (o == null) {");
    this.println("      return false;");
    this.println("    }");
    this.println("    if (this == o) {");
    this.println("      return true;");
    this.println("    }");
    this.println("    if (o instanceof %s) {", new Object[] {typeName});
    if (hasDeclareMember) {
      this.println("      %s that = (%s) o;", new Object[] {typeName, typeName});
      this.print("      return ");
      boolean started = false;
      Iterator var5 = entity.fields.iterator();

      while (var5.hasNext()) {
        EntityField field = (EntityField) var5.next();
        if (started) {
          this.print(" &&\n          ");
        } else {
          started = true;
        }

        if (field.isArray) {
          this.print(
              "java.util.Arrays.equals(%s(), that.%s())",
              new Object[] {field.getter, field.getter});
        } else {
          this.print(
              "java.util.Objects.equals(%s(), that.%s())",
              new Object[] {field.getter, field.getter});
        }
      }

      this.println(";");
    } else {
      this.print("      return true;");
    }

    this.println("    } else {");
    this.println("      return false;");
    this.println("    }");
    this.println("  }");
    this.println();
    this.println("  @Override");
    this.println("  public String toString() {");
    this.println("    StringBuilder sb = new StringBuilder();");
    this.println("    sb.append(\"class \").append(getClass().getName()).append(\" {\\n\");");
    Iterator var7 = entity.fields.iterator();

    while (var7.hasNext()) {
      EntityField field = (EntityField) var7.next();
      this.println(
          "    sb.append(\"  %s: \").append(toIndentedString(%s())).append(\"\\n\");",
          new Object[] {field.fieldName, field.getter});
    }

    this.println("    sb.append(\"}\");");
    this.println("    return sb.toString();");
    this.println("  }");
    if (hasDeclareMember) {
      this.println();
      this.println("  private String toIndentedString(java.lang.Object o) {");
      this.println("    if (o == null) {");
      this.println("      return \"null\";");
      this.println("    }");
      this.println("    return o.toString().replace(\"\\n\", \"\\n  \");");
      this.println("  }");
    }
  }
}
