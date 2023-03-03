package com.yichi.infra.support.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.yichi.infra.support.mybatis.annotations.Column;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes({"com.yichi.infra.support.entity.Entity"})
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class EntityProcessor extends AbstractProcessor {

  private static final List<String> ForbiddenEntityNameSuffixes =
      ImmutableList.of("Impl", "Builder", "BuilderImpl");
  private static final Set<String> PrimitiveTypeNames =
      ImmutableSet.of(
          "float", "int", "double", "long", "char", "boolean", new String[] {"byte", "short"});
  private static final String EntityClassName = "com.yichi.infra.support.entity.Entity";
  private static final String ColumnAnnotationClassName =
      "com.yichi.infra.support.mybatis.annotations.Column";
  private static final String RequiredAnnotationClassName =
      "com.yichi.infra.support.entity.Required";
  private static final Pattern ANNOTATION_MATCHER = Pattern.compile("@[^\\s]*");
  private Types typeUtils;
  private ProcessingEnvironment processingEnvironment;

  public EntityProcessor() {}

  public synchronized void init(ProcessingEnvironment env) {
    this.processingEnvironment = env;
    this.typeUtils = env.getTypeUtils();
    super.init(env);
  }

  private static AnnotationMirror getAnnotation(Element element, String annotationName) {
    List<? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
    Iterator var3 = annotations.iterator();

    AnnotationMirror annotation;
    do {
      if (!var3.hasNext()) {
        return null;
      }

      annotation = (AnnotationMirror) var3.next();
    } while (!annotationName.equals(annotation.getAnnotationType().toString()));

    return annotation;
  }

  private static String getAnnotationValue(AnnotationMirror annotation, String key) {
    if (annotation == null) {
      return null;
    } else {
      Iterator var2 = annotation.getElementValues().entrySet().iterator();

      Entry entry;
      do {
        if (!var2.hasNext()) {
          return null;
        }

        entry = (Entry) var2.next();
      } while (!key.equals(((ExecutableElement) entry.getKey()).getSimpleName().toString()));

      return ((AnnotationValue) entry.getValue()).getValue().toString();
    }
  }

  private static String getAnnotationValue(Element element, String annotationName, String key) {
    AnnotationMirror annotation = getAnnotation(element, annotationName);
    return getAnnotationValue(annotation, key);
  }

  private static boolean classExists(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException var2) {
      return false;
    }
  }

  private static String getTypeWithoutAnnotation(TypeMirror type) {
    String name = type.toString();
    return ANNOTATION_MATCHER.matcher(name).replaceAll("").trim();
  }

  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    List<EntityProcessor.EntityClass> entities = new ArrayList();
    Iterator var4 = annotations.iterator();

    while (var4.hasNext()) {
      TypeElement annotation = (TypeElement) var4.next();
      Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
      Map<String, TypeElement> processedElements = new HashMap();
      Iterator var8 = annotatedElements.iterator();

      Element element;
      while (var8.hasNext()) {
        element = (Element) var8.next();
        TypeElement ele = (TypeElement) element;
        processedElements.put(ele.getQualifiedName().toString(), ele);
      }

      var8 = annotatedElements.iterator();

      while (var8.hasNext()) {
        element = (Element) var8.next();
        String className = ((TypeElement) element).getQualifiedName().toString();
        Iterator var11 = ForbiddenEntityNameSuffixes.iterator();

        while (var11.hasNext()) {
          String suffix = (String) var11.next();
          if (className.endsWith(suffix)) {
            throw new RuntimeException(
                String.format("Entity class name must not end with '%s'", suffix));
          }
        }

        EntityProcessor.EntityClass entity = new EntityProcessor.EntityClass();
        int lastDotIndex = className.lastIndexOf(46);
        if (lastDotIndex >= 0) {
          entity.packageName = className.substring(0, lastDotIndex);
          entity.interfaceName = className.substring(lastDotIndex + 1);
        } else {
          entity.interfaceName = className;
        }

        entity.name = className;
        entity.implClassName = entity.interfaceName + "Impl";
        entity.builderName = entity.interfaceName + "Builder";
        entity.builderImplClassName = entity.interfaceName + "BuilderImpl";
        if (classExists("com.yichi.infra.support.entity.Entity")) {
          Entity entityAnnotation = (Entity) element.getAnnotation(Entity.class);
          entity.entityBuilderName = entityAnnotation.builder();
          if (entityAnnotation.deprecatedFields() != null) {
            String[] var14 = entityAnnotation.deprecatedFields();
            int var15 = var14.length;

            for (int var16 = 0; var16 < var15; ++var16) {
              String field = var14[var16];
              entity.deprecatedFields.add(field);
            }
          }
        } else {
          String builderName =
              getAnnotationValue(element, "com.yichi.infra.support.entity.Entity", "builder");
          if (builderName == null) {
            builderName = "com.yichi.infra.support.EntityBuilder";
          }

          entity.entityBuilderName = builderName;
        }

        this.processElement(element, entity, processedElements, false);
        entities.add(entity);
      }
    }

    if (!entities.isEmpty()) {
      try {
        var4 = entities.iterator();

        while (var4.hasNext()) {
          EntityProcessor.EntityClass entity = (EntityProcessor.EntityClass) var4.next();
          this.writeImpl(entity);
          this.writeBuilder(entity, entities);
          this.writeBuilderImpl(entity, entities);
        }

        this.writeEntityBuilder(entities);
      } catch (IOException var18) {
        throw new RuntimeException(var18);
      }
    }

    return true;
  }

  private void processElement(
      Element element,
      EntityProcessor.EntityClass entity,
      Map<String, TypeElement> processedElements,
      boolean isAlreadyAnEntity) {
    Iterator var5 = element.getEnclosedElements().iterator();

    while (true) {
      while (true) {
        while (true) {
          while (true) {
            while (true) {
              while (true) {
                Element ele;
                do {
                  if (!var5.hasNext()) {
                    var5 = this.typeUtils.directSupertypes(element.asType()).iterator();

                    while (true) {
                      TypeMirror supertype;
                      do {
                        if (!var5.hasNext()) {
                          return;
                        }

                        supertype = (TypeMirror) var5.next();
                      } while (supertype.toString().equals("java.lang.Object"));

                      Element superElement = ((DeclaredType) supertype).asElement();
                      boolean isEntity = isEntity(superElement);
                      if (isEntity) {
                        EntityProcessor.SuperType superType = new EntityProcessor.SuperType();
                        superType.isDirect = !isAlreadyAnEntity;
                        superType.name = superElement.toString();
                        entity.superEntities.add(superType);
                      }

                      this.processElement(
                          superElement, entity, processedElements, isAlreadyAnEntity || isEntity);
                    }
                  }

                  ele = (Element) var5.next();
                } while (ele.getKind() != ElementKind.METHOD);

                String simpleName = ele.getSimpleName().toString();
                TypeMirror returnType = ((ExecutableElement) ele).getReturnType();
                String fieldName;
                String error;
                if (!isEntity(element)
                    || !simpleName.equals("toBuilder")
                    || !((ExecutableElement) ele).getParameters().isEmpty()) {
                  if (!simpleName.equals("clone")
                      || !((ExecutableElement) ele).getParameters().isEmpty()) {
                    if (!simpleName.equals("isEmpty")
                        || !((ExecutableElement) ele).getParameters().isEmpty()) {
                      if (!simpleName.equals("isHollow")
                          || !((ExecutableElement) ele).getParameters().isEmpty()) {
                        if (simpleName.startsWith("get")) {
                          fieldName = toName(simpleName);
                          if (entity.deprecatedFields.contains(fieldName)) {
                            error =
                                String.format(
                                    "%s.%s is marked as deprecated, please do not use it any more",
                                    entity.interfaceName, fieldName);
                            throw new IllegalArgumentException(error);
                          }

                          if (returnType.getKind().isPrimitive()) {
                            error =
                                String.format(
                                    "%s.%s is defined as a primitive type, which is disallowed,"
                                        + " see %s for further details.",
                                    entity.interfaceName,
                                    fieldName,
                                    "https://yaocheng.feishu.cn/docs/doccnVYlgEtqzGvfoO4UxCRn4ug#H1o4YD");
                            throw new IllegalArgumentException(error);
                          }

                          if (!entity.hasField(fieldName)) {
                            EntityProcessor.EntityField field = new EntityProcessor.EntityField();
                            field.fieldName = fieldName;
                            AnnotationMirror requiredAnnotation;
                            if (classExists("com.yichi.infra.support.mybatis.annotations.Column")) {
                              Column column = (Column) ele.getAnnotation(Column.class);
                              if (column != null) {
                                field.columnAnnotation = new EntityProcessor.ColumnAnnotationInfo();
                                field.columnAnnotation.isJson = column.json();
                              }
                            } else {
                              requiredAnnotation =
                                  getAnnotation(
                                      ele, "com.yichi.infra.support.mybatis.annotations.Column");
                              if (requiredAnnotation != null) {
                                field.columnAnnotation = new EntityProcessor.ColumnAnnotationInfo();
                                field.columnAnnotation.isJson =
                                    "true".equals(getAnnotationValue(requiredAnnotation, "json"));
                              }
                            }

                            if (classExists("com.yichi.infra.support.entity.Required")) {
                              Required required = (Required) ele.getAnnotation(Required.class);
                              if (required != null) {
                                field.requiredAnnotation =
                                    new EntityProcessor.RequiredAnnotationInfo();
                                field.requiredAnnotation.defaultValue = required.defaultValue();
                              }
                            } else {
                              requiredAnnotation =
                                  getAnnotation(ele, "com.yichi.infra.support.entity.Required");
                              if (requiredAnnotation != null) {
                                field.requiredAnnotation =
                                    new EntityProcessor.RequiredAnnotationInfo();
                                String defaultValue =
                                    getAnnotationValue(requiredAnnotation, "defaultValue");
                                if (defaultValue != null) {
                                  field.requiredAnnotation.defaultValue = defaultValue;
                                }
                              }
                            }

                            field.isSuperDeclared = isAlreadyAnEntity || isOverride(ele);
                            field.setter = toSetter(field.fieldName);
                            field.getter = toGetter(field.fieldName);
                            field.typeName = getTypeWithoutAnnotation(returnType);
                            field.javadoc = this.processingEnv.getElementUtils().getDocComment(ele);
                            if (returnType instanceof DeclaredType) {
                              field.typeClassName =
                                  ((DeclaredType) returnType).asElement().toString();
                              field.isGenericType =
                                  !((DeclaredType) returnType).getTypeArguments().isEmpty();
                              field.isOptional =
                                  ((DeclaredType) returnType)
                                      .asElement()
                                      .toString()
                                      .equals("java.util.Optional");
                              if (field.isGenericType) {
                                field.genericTypes = ((DeclaredType) returnType).getTypeArguments();
                              }

                              field.isArray = false;
                            } else {
                              field.typeClassName = returnType.toString();
                              field.isGenericType = false;
                              field.isOptional = false;
                              field.isArray = true;
                            }

                            String returnTypeName = returnType.toString();
                            if (processedElements.containsKey(returnTypeName)) {
                              field.entityFieldType = EntityProcessor.EntityFieldType.ENTITY;
                            } else if (field.typeClassName.equals("java.util.List")) {
                              field.entityFieldType = EntityProcessor.EntityFieldType.LIST;
                            } else if (field.typeClassName.equals("java.util.Map")) {
                              field.entityFieldType = EntityProcessor.EntityFieldType.MAP;
                            } else if (field.typeClassName.equals("java.util.Set")) {
                              field.entityFieldType = EntityProcessor.EntityFieldType.SET;
                            } else {
                              field.entityFieldType = EntityProcessor.EntityFieldType.BASIC;
                            }

                            if (PrimitiveTypeNames.contains(field.fieldName)) {
                              field.fieldName = field.fieldName + "_";
                            }

                            entity.fields.add(field);
                          }
                        } else if (!ele.getModifiers().contains(Modifier.DEFAULT)
                            && !ele.getModifiers().contains(Modifier.STATIC)) {
                          throw new RuntimeException(
                              String.format(
                                  "Unsupported method \"%s\" in class \"%s\"",
                                  simpleName, element.toString()));
                        }
                      } else {
                        if (!"boolean".equals(returnType.toString())) {
                          throw new RuntimeException(
                              String.format(
                                  "\"%s.isHollow\" method must returns \"boolean\", but it returns"
                                      + " \"%s\"",
                                  element.toString(), returnType.toString()));
                        }

                        entity.hasIsHollow = true;
                      }
                    } else {
                      if (!"boolean".equals(returnType.toString())) {
                        throw new RuntimeException(
                            String.format(
                                "\"%s.isEmpty\" method must returns \"boolean\", but it returns"
                                    + " \"%s\"",
                                element.toString(), returnType.toString()));
                      }

                      entity.hasIsEmpty = true;
                    }
                  } else {
                    fieldName = ((TypeElement) element).getQualifiedName().toString();
                    error = fieldName.substring(fieldName.lastIndexOf(46) + 1);
                    if (!error.equals(returnType.toString())
                        && !fieldName.equals(returnType.toString())) {
                      throw new RuntimeException(
                          String.format(
                              "\"%s.clone\" method must returns \"%s\", but it returns \"%s\"",
                              element.toString(), element.toString(), returnType.toString()));
                    }
                  }
                } else {
                  fieldName = ((TypeElement) element).getQualifiedName().toString() + "Builder";
                  error = fieldName.substring(fieldName.lastIndexOf(46) + 1);
                  if (!error.equals(returnType.toString())
                      && !fieldName.equals(returnType.toString())) {
                    throw new RuntimeException(
                        String.format(
                            "\"%s.toBuilder\" method must returns \"%s\", but it returns \"%s\"",
                            element.toString(), error, returnType.toString()));
                  }

                  entity.hasToBuilder = true;
                }
              }
            }
          }
        }
      }
    }
  }

  private void writeImpl(EntityProcessor.EntityClass entity) throws IOException {
    JavaFileObject file =
        this.processingEnv
            .getFiler()
            .createSourceFile(entity.packageName + "." + entity.implClassName, new Element[0]);
    Writer writer = file.openWriter();
    (new EntityImplGenerator(this.processingEnvironment, writer)).generate(entity);
    writer.close();
  }

  private void writeBuilder(
      EntityProcessor.EntityClass entity, List<EntityProcessor.EntityClass> entities)
      throws IOException {
    JavaFileObject file =
        this.processingEnv
            .getFiler()
            .createSourceFile(entity.packageName + "." + entity.builderName, new Element[0]);
    Writer writer = file.openWriter();
    (new BuilderGenerator(this.processingEnvironment, writer)).generate(entity, entities);
    writer.close();
  }

  private void writeBuilderImpl(
      EntityProcessor.EntityClass entity, List<EntityProcessor.EntityClass> entities)
      throws IOException {
    JavaFileObject file =
        this.processingEnv
            .getFiler()
            .createSourceFile(
                entity.packageName + "." + entity.builderImplClassName, new Element[0]);
    Writer writer = file.openWriter();
    (new BuilderImplGenerator(this.processingEnvironment, writer)).generate(entity, entities);
    writer.close();
  }

  private void writeEntityBuilder(List<EntityProcessor.EntityClass> entities) throws IOException {
    if (!entities.isEmpty()) {
      String entityBuilderName = ((EntityProcessor.EntityClass) entities.get(0)).entityBuilderName;
      Iterator var3 = entities.iterator();

      EntityProcessor.EntityClass entity;
      do {
        if (!var3.hasNext()) {
          JavaFileObject file =
              this.processingEnv.getFiler().createSourceFile(entityBuilderName, new Element[0]);
          Writer writer = file.openWriter();
          (new EntityBuilderGenerator(this.processingEnvironment, writer))
              .generate(entities, entityBuilderName);
          writer.close();
          return;
        }

        entity = (EntityProcessor.EntityClass) var3.next();
      } while (entityBuilderName.equals(entity.entityBuilderName));

      String error = "All entities in one project must use same builder in @Entity";
      System.err.println(error);
      Iterator var6 = entities.iterator();

      while (var6.hasNext()) {
        EntityProcessor.EntityClass e = (EntityProcessor.EntityClass) var6.next();
        System.err.println(
            String.format("  %s.%s uses %s", e.packageName, e.interfaceName, e.entityBuilderName));
      }

      throw new RuntimeException(error);
    }
  }

  public static String toUpperName(String name) {
    String var10000 = name.substring(0, 1).toUpperCase();
    return var10000 + name.substring(1);
  }

  private static String toGetter(String name) {
    return "get" + toUpperName(name);
  }

  private static String toSetter(String name) {
    return "set" + toUpperName(name);
  }

  private static String toName(String method) {
    String name = method.substring(3);
    String var10000 = name.substring(0, 1).toLowerCase();
    return var10000 + name.substring(1);
  }

  private static boolean isEntity(Element element) {
    return hasAnnotation(element, "com.yichi.infra.support.entity.Entity");
  }

  private static boolean isOverride(Element element) {
    return hasAnnotation(element, "java.lang.Override");
  }

  private static boolean hasAnnotation(Element element, String name) {
    Iterator var2 = element.getAnnotationMirrors().iterator();

    AnnotationMirror annotationMirror;
    do {
      if (!var2.hasNext()) {
        return false;
      }

      annotationMirror = (AnnotationMirror) var2.next();
    } while (!annotationMirror.getAnnotationType().toString().equals(name));

    return true;
  }

  static class EntityClass {
    String name;
    String packageName;
    String interfaceName;
    String implClassName;
    String builderName;
    String builderImplClassName;
    Set<String> deprecatedFields = new HashSet();
    List<EntityProcessor.EntityField> fields = new ArrayList();
    List<EntityProcessor.SuperType> superEntities = new ArrayList();
    String entityBuilderName;
    boolean hasToBuilder;
    boolean hasIsEmpty;
    boolean hasIsHollow;

    EntityClass() {}

    boolean hasField(String name) {
      Iterator var2 = this.fields.iterator();

      EntityProcessor.EntityField field;
      do {
        if (!var2.hasNext()) {
          return false;
        }

        field = (EntityProcessor.EntityField) var2.next();
      } while (!field.fieldName.equals(name));

      return true;
    }
  }

  static class SuperType {
    String name;
    boolean isDirect;

    SuperType() {}
  }

  static class EntityField {
    EntityProcessor.EntityFieldType entityFieldType;
    String javadoc;
    String typeName;
    String typeClassName;
    String fieldName;
    String getter;
    String setter;
    EntityProcessor.ColumnAnnotationInfo columnAnnotation;
    EntityProcessor.RequiredAnnotationInfo requiredAnnotation;
    boolean isSuperDeclared = false;
    boolean isGenericType = false;
    boolean isOptional = false;
    boolean isArray = false;
    List<? extends TypeMirror> genericTypes = new ArrayList();

    EntityField() {}
  }

  static class RequiredAnnotationInfo {
    String defaultValue = "";

    RequiredAnnotationInfo() {}
  }

  static class ColumnAnnotationInfo {
    boolean isJson = false;

    ColumnAnnotationInfo() {}
  }

  static enum EntityFieldType {
    BASIC,
    ENTITY,
    LIST,
    MAP,
    SET;

    private EntityFieldType() {}
  }
}
