package com.yichi.infra.support.entity;

import com.yichi.infra.support.entity.EntityProcessor.EntityClass;
import com.yichi.infra.support.entity.EntityProcessor.EntityField;
import com.yichi.infra.support.entity.EntityProcessor.EntityFieldType;
import com.yichi.infra.support.entity.EntityProcessor.SuperType;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

public class BuilderGenerator extends EntityGenerator {
  public BuilderGenerator(ProcessingEnvironment processingEnvironment, Writer writer) {
    super(processingEnvironment, writer);
  }

  public void generateFromEntity(EntityClass entity) {
    this.println(
        "\n  public static %s from(%s entity) {",
        new Object[] {entity.builderName, entity.interfaceName});
    this.print("    return new %s()", new Object[] {entity.builderImplClassName});
    Iterator var2 = entity.fields.iterator();

    while (var2.hasNext()) {
      EntityField field = (EntityField) var2.next();
      this.print("\n        .%s(entity.%s())", new Object[] {field.setter, field.getter});
    }

    this.println(";");
    this.println("  }");
  }

  public void generateJSONMethods(EntityClass entity) {
    this.println("");
    this.println("  public static %s fromJSON(String json) {", new Object[] {entity.interfaceName});
    this.println(
        "    return com.yichi.infra.support.entity.EntitySerializer.get().deserialize(json,"
            + " %s.class);",
        new Object[] {entity.interfaceName});
    this.println("  }");
    this.println("");
    this.println("  public static String toJSON(%s entity) {", new Object[] {entity.interfaceName});
    this.println(
        "    return com.yichi.infra.support.entity.EntitySerializer.get().serialize(entity);");
    this.println("  }");
    this.println("");
  }

  public void generateMergeMethods(EntityClass entity, List<EntityClass> entities) {
    Iterator var3 = entity.superEntities.iterator();

    while (true) {
      SuperType superType;
      EntityClass superEntity;
      Iterator var6;
      do {
        if (!var3.hasNext()) {
          return;
        }

        superType = (SuperType) var3.next();
        superEntity = null;
        var6 = entities.iterator();

        while (var6.hasNext()) {
          EntityClass superClass = (EntityClass) var6.next();
          if (superClass.name.equals(superType.name)) {
            superEntity = superClass;
            break;
          }
        }
      } while (superEntity == null);

      this.println();
      this.println(
          "  public static %s from(%s entity) {",
          new Object[] {entity.builderName, superType.name});
      this.print("    return new %s()", new Object[] {entity.builderImplClassName});
      var6 = superEntity.fields.iterator();

      while (var6.hasNext()) {
        EntityField field = (EntityField) var6.next();
        this.print("\n        .%s(entity.%s())", new Object[] {field.setter, field.getter});
      }

      this.println(";");
      this.println("  }");
      this.println();
      if (!superType.isDirect) {
        this.println("  @Override");
      }

      this.println(
          "  public %s merge(%s entity);", new Object[] {entity.builderName, superType.name});
    }
  }

  private void beginNewFieldMethod(EntityField field) {
    this.println();
    this.printJavadoc(field.javadoc);
    if (field.isSuperDeclared) {
      this.println("  @Override");
    }
  }

  public void generate(EntityClass entity, List<EntityClass> entities) {
    this.generatePackage(entity);
    this.generateGeneratedAnnotation();
    this.print("public interface %s ", new Object[] {entity.builderName});
    boolean started = false;
    Iterator var4 = entity.superEntities.iterator();

    while (var4.hasNext()) {
      SuperType superType = (SuperType) var4.next();
      if (superType.isDirect) {
        if (started) {
          this.print(", ");
        } else {
          started = true;
          this.print("extends ");
        }

        this.print("%sBuilder", new Object[] {superType.name});
      }
    }

    this.println(" {");
    this.println(
        "\n  public static %s newBuilder() {\n    return new %s();\n  }",
        new Object[] {entity.builderName, entity.builderImplClassName});
    this.generateFromEntity(entity);
    this.generateJSONMethods(entity);
    this.generateMergeMethods(entity, entities);
    var4 = entity.fields.iterator();

    while (var4.hasNext()) {
      EntityField field = (EntityField) var4.next();
      this.beginNewFieldMethod(field);
      this.println("  %s %s();", new Object[] {field.typeName, field.getter});
      this.beginNewFieldMethod(field);
      this.println(
          "  %s %s(%s %s);",
          new Object[] {entity.builderName, field.setter, field.typeName, field.fieldName});
      this.beginNewFieldMethod(field);
      this.println(
          "  %s %s(%s %s);",
          new Object[] {entity.builderName, field.fieldName, field.typeName, field.fieldName});
      if (field.isOptional) {
        this.beginNewFieldMethod(field);
        this.println(
            "  %s %s(%s %s);",
            new Object[] {
              entity.builderName,
              field.setter,
              ((TypeMirror) field.genericTypes.get(0)).toString(),
              field.fieldName
            });
        this.beginNewFieldMethod(field);
        this.println(
            "  %s %s(%s %s);",
            new Object[] {
              entity.builderName,
              field.fieldName,
              ((TypeMirror) field.genericTypes.get(0)).toString(),
              field.fieldName
            });
      }

      if (field.entityFieldType == EntityFieldType.LIST) {
        this.beginNewFieldMethod(field);
        this.println(
            "  %s add%sItem(%s item);",
            new Object[] {
              entity.builderName,
              EntityProcessor.toUpperName(field.fieldName),
              ((TypeMirror) field.genericTypes.get(0)).toString()
            });
      } else if (field.entityFieldType == EntityFieldType.MAP) {
        this.beginNewFieldMethod(field);
        this.println(
            "  %s put%sItem(%s key, %s value);",
            new Object[] {
              entity.builderName,
              EntityProcessor.toUpperName(field.fieldName),
              ((TypeMirror) field.genericTypes.get(0)).toString(),
              ((TypeMirror) field.genericTypes.get(1)).toString()
            });
      }
    }

    this.println("");
    if (!entity.superEntities.isEmpty()) {
      this.println("  @Override");
    }

    this.println("  %s build();", new Object[] {entity.interfaceName});
    this.println("}");
    this.println();
  }
}
