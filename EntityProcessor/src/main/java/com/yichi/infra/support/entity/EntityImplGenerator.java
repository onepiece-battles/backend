package com.yichi.infra.support.entity;

import com.yichi.infra.support.entity.EntityProcessor.EntityClass;
import com.yichi.infra.support.entity.EntityProcessor.EntityField;
import java.io.Writer;
import java.util.Iterator;
import javax.annotation.processing.ProcessingEnvironment;

public class EntityImplGenerator extends EntityGenerator {

  private static final String EntityImplInterface = "com.yichi.infra.support.entity.EntityImpl";

  public void generateGetterWithDefaultValue(EntityClass entity, EntityField field) {
    String type = field.typeName;
    String name = field.fieldName;
    this.println();
    this.println("  @Override");
    this.println("  public %s %s() {", new Object[] {type, field.getter});
    if (field.requiredAnnotation == null) {
      this.println("    return %s;", new Object[] {name});
    } else {
      this.println(
          "    return %s == null ? %s : %s;",
          new Object[] {name, this.getDefaultValue(entity, field), name});
    }

    this.println("  }");
  }

  public EntityImplGenerator(ProcessingEnvironment processingEnvironment, Writer writer) {
    super(processingEnvironment, writer);
  }

  public void generateImmutableSetter(EntityClass entity, EntityField field) {
    String type = field.typeName;
    String name = field.fieldName;
    this.println();
    this.println(
        "  public %s %s(%s %s) {", new Object[] {entity.implClassName, field.setter, type, name});
    if (field.columnAnnotation != null && field.columnAnnotation.isJson && field.isGenericType) {
      this.println(
          "    // Using generic type for json column is not safe, convert it to right type.");
      this.println("    if (%s != null) {", new Object[] {name});
      this.println(
          "      %s = com.yichi.infra.support.entity.EntitySerializer.get()", new Object[] {name});
      this.println("        .getObjectMapper().convertValue(");
      this.println("          %s,", new Object[] {name});
      this.println(
          "          new com.fasterxml.jackson.core.type.TypeReference<%s>() {});",
          new Object[] {field.typeName});
      this.println("    }");
    }

    switch (field.entityFieldType) {
      case BASIC:
        this.println("    this.%s = %s;", new Object[] {name, name});
        break;
      case ENTITY:
        this.println(
            "    this.%s = %s == null ? null : new %sImpl(%s);",
            new Object[] {name, name, field.typeName, name});
        break;
      case LIST:
        this.println(
            "    this.%s = %s == null ? null : com.google.common.collect.ImmutableList.copyOf(%s);",
            new Object[] {name, name, name});
        break;
      case SET:
        this.println(
            "    this.%s = %s == null ? null : com.google.common.collect.ImmutableSet.copyOf(%s);",
            new Object[] {name, name, name});
        break;
      case MAP:
        this.println(
            "    this.%s = %s == null ? null : com.google.common.collect.ImmutableMap.copyOf(%s);",
            new Object[] {name, name, name});
    }

    this.println("    return this;");
    this.println("  }");
  }

  public void generate(EntityClass entity) {
    this.generatePackage(entity);
    this.generateGeneratedAnnotation();
    this.println(
        "public class %s implements %s, %s {",
        new Object[] {
          entity.implClassName, entity.interfaceName, "com.yichi.infra.support.entity.EntityImpl"
        });
    this.generateMembers(entity);
    this.generateDefaultConstructor("public", entity.implClassName);
    this.generateAllArgsConstructor("public", entity.implClassName, entity.fields);
    this.println();
    this.println(
        "  public %s(%s entity) {", new Object[] {entity.implClassName, entity.interfaceName});
    Iterator var2 = entity.fields.iterator();

    EntityField field;
    while (var2.hasNext()) {
      field = (EntityField) var2.next();
      this.println("    %s(entity.%s());", new Object[] {field.setter, field.getter});
    }

    this.println("  }");
    var2 = entity.fields.iterator();

    while (var2.hasNext()) {
      field = (EntityField) var2.next();
      this.generateGetterWithDefaultValue(entity, field);
      this.generateImmutableSetter(entity, field);
    }

    this.println();
    this.println("  @Override");
    this.println("  @java.lang.SuppressWarnings(\"unchecked\")");
    this.println("  public Object get(String name) throws NoSuchFieldException {");
    var2 = entity.fields.iterator();

    while (var2.hasNext()) {
      field = (EntityField) var2.next();
      this.println("    if (\"%s\".equals(name)) {", new Object[] {field.fieldName});
      this.println("      return %s();", new Object[] {field.getter});
      this.println("    }");
    }

    this.println("    throw new NoSuchFieldException(\"no such field:\" + name);");
    this.println("  }");
    this.println();
    this.println("  @Override");
    this.println("  @java.lang.SuppressWarnings(\"unchecked\")");
    this.println("  public void set(String name, Object obj) throws NoSuchFieldException {");
    var2 = entity.fields.iterator();

    while (var2.hasNext()) {
      field = (EntityField) var2.next();
      this.println("    if (\"%s\".equals(name)) {", new Object[] {field.fieldName});
      if (field.typeName.equals("java.lang.Object")) {
        this.println("      %s(obj);", new Object[] {field.setter});
      } else {
        this.println("      %s((%s)obj);", new Object[] {field.setter, field.typeName});
      }

      this.println("      return;");
      this.println("    }");
    }

    this.println("    throw new NoSuchFieldException(\"no such field:\" + name);");
    this.println("  }");
    if (entity.hasToBuilder) {
      this.println();
      this.println("  @Override");
      this.println("  public %s toBuilder() {", new Object[] {entity.builderName});
      this.println("    return %s.from(this);", new Object[] {entity.builderName});
      this.println("  }");
    }

    boolean started;
    Iterator var6;
    if (entity.hasIsEmpty) {
      this.println();
      this.println("  @Override");
      this.println("  public boolean isEmpty() {");
      this.print("    return ");
      started = false;

      for (var6 = entity.fields.iterator();
          var6.hasNext();
          this.print("%s() == null", new Object[] {field.getter})) {
        field = (EntityField) var6.next();
        if (!started) {
          started = true;
        } else {
          this.print(" &&\n        ");
        }
      }

      if (!started) {
        this.print("true");
      }

      this.println(";");
      this.println("  }");
    }

    if (entity.hasIsHollow) {
      this.println();
      this.println("  @Override");
      this.println("  public boolean isHollow() {");
      this.print("    return ");
      started = false;
      var6 = entity.fields.iterator();

      while (var6.hasNext()) {
        field = (EntityField) var6.next();
        if (!field.fieldName.equals("id")) {
          if (!started) {
            started = true;
          } else {
            this.print(" &&\n        ");
          }

          this.print("%s() == null", new Object[] {field.getter});
        }
      }

      if (!started) {
        this.print("true");
      }

      this.println(";");
      this.println("  }");
    }

    this.println();
    this.println("  @Override");
    this.println("  public %s clone() {", new Object[] {entity.interfaceName});
    this.println("    return %s.from(this).build();", new Object[] {entity.builderName});
    this.println("  }");
    this.generateEqualsAndHashCodeAndToString(entity, entity.interfaceName);
    this.println("}");
  }
}
