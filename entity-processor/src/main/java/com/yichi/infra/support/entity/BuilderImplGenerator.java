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

public class BuilderImplGenerator extends EntityGenerator {
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
      this.println("  @Override");
      this.println(
          "  public %s merge(%s entity) {", new Object[] {entity.builderName, superType.name});
      this.print("    return this");
      var6 = superEntity.fields.iterator();

      while (var6.hasNext()) {
        EntityField field = (EntityField) var6.next();
        this.print("\n        .%s(entity.%s())", new Object[] {field.setter, field.getter});
      }

      this.println(";");
      this.println("  }");
    }
  }

  public BuilderImplGenerator(ProcessingEnvironment processingEnvironment, Writer writer) {
    super(processingEnvironment, writer);
  }

  public void generate(EntityClass entity, List<EntityClass> entities) {
    this.generatePackage(entity);
    this.generateGeneratedAnnotation();
    this.println(
        "public class %s implements %s {",
        new Object[] {entity.builderImplClassName, entity.builderName});
    this.generateMembers(entity);
    this.generateMergeMethods(entity, entities);
    this.generateDefaultConstructor("public", entity.builderImplClassName);
    this.generateAllArgsConstructor("public", entity.builderImplClassName, entity.fields);
    this.println();
    this.println(
        "  public %s(%s entity) {",
        new Object[] {entity.builderImplClassName, entity.interfaceName});
    Iterator var3 = entity.fields.iterator();

    EntityField field;
    while (var3.hasNext()) {
      field = (EntityField) var3.next();
      this.println("    %s(entity.%s());", new Object[] {field.setter, field.getter});
    }

    this.println("  }");
    var3 = entity.fields.iterator();

    while (var3.hasNext()) {
      field = (EntityField) var3.next();
      this.generateGetter(field);
      this.println();
      this.println("  @Override");
      this.println(
          "  public %s %s(%s %s) {",
          new Object[] {entity.builderName, field.setter, field.typeName, field.fieldName});
      this.println("    this.%s = %s;", new Object[] {field.fieldName, field.fieldName});
      this.println("    return this;");
      this.println("  }");
      this.println();
      this.println("  @Override");
      this.println(
          "  public %s %s(%s %s) {",
          new Object[] {entity.builderName, field.fieldName, field.typeName, field.fieldName});
      this.println("    return %s(%s);", new Object[] {field.setter, field.fieldName});
      this.println("  }");
      if (field.isOptional) {
        String type = ((TypeMirror) field.genericTypes.get(0)).toString();
        this.println();
        this.println("  @Override");
        this.println(
            "  public %s %s(%s %s) {",
            new Object[] {entity.builderName, field.setter, type, field.fieldName});
        this.println(
            "    this.%s = %s == null ? java.util.Optional.empty() : java.util.Optional.of(%s);",
            new Object[] {field.fieldName, field.fieldName, field.fieldName});
        this.println("    return this;");
        this.println("  }");
        this.println();
        this.println("  @Override");
        this.println(
            "  public %s %s(%s %s) {",
            new Object[] {
              entity.builderName,
              field.fieldName,
              ((TypeMirror) field.genericTypes.get(0)).toString(),
              field.fieldName
            });
        this.println("    return %s(%s);", new Object[] {field.setter, field.fieldName});
        this.println("  }");
        this.println();
      }

      if (field.entityFieldType == EntityFieldType.LIST) {
        this.println();
        this.println("  @Override");
        this.println(
            "  public %s add%sItem(%s item) {",
            new Object[] {
              entity.builderName,
              EntityProcessor.toUpperName(field.fieldName),
              ((TypeMirror) field.genericTypes.get(0)).toString()
            });
        this.println("  if (%s == null) {", new Object[] {field.fieldName});
        this.println("    %s = new java.util.ArrayList<>();", new Object[] {field.fieldName});
        this.println("  }");
        this.println("  %s.add(item);", new Object[] {field.fieldName});
        this.println("  return this;");
        this.println("}");
      } else if (field.entityFieldType == EntityFieldType.MAP) {
        this.println();
        this.println("  @Override");
        this.println(
            "  public %s put%sItem(%s key, %s value) {",
            new Object[] {
              entity.builderName,
              EntityProcessor.toUpperName(field.fieldName),
              ((TypeMirror) field.genericTypes.get(0)).toString(),
              ((TypeMirror) field.genericTypes.get(1)).toString()
            });
        this.println("  if (%s == null) {", new Object[] {field.fieldName});
        this.println("    %s = new java.util.HashMap<>();", new Object[] {field.fieldName});
        this.println("  }");
        this.println("  %s.put(key, value);", new Object[] {field.fieldName});
        this.println("  return this;");
        this.println("}");
      }
    }

    this.println();
    this.println("  @Override");
    this.println("  public %s clone() {", new Object[] {entity.builderName});
    this.println("    return %s.from(this.build());", new Object[] {entity.builderName});
    this.println("  }");
    this.println("");
    this.println("  @Override");
    this.println("  public %s build() {", new Object[] {entity.interfaceName});
    this.println(
        "    return new %s(%s);",
        new Object[] {entity.implClassName, this.toParameters(entity.fields)});
    this.println("  }");
    this.generateEqualsAndHashCodeAndToString(entity, entity.builderName);
    this.println("}");
  }
}
