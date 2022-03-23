package com.yichi.infra.support.entity;

import com.yichi.infra.support.entity.EntityProcessor.EntityClass;
import java.io.Writer;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;

public class EntityBuilderGenerator extends BaseCodeGenerator {
  EntityBuilderGenerator(ProcessingEnvironment processingEnvironment, Writer writer) {
    super(processingEnvironment, writer);
  }

  void generate(List<EntityClass> entities, String name) {
    String packageName = "";
    String builderName = name;
    int index = name.lastIndexOf(46);
    if (index >= 0) {
      packageName = name.substring(0, index);
      builderName = name.substring(index + 1);
    }

    this.println("package %s;", new Object[] {packageName});
    this.println();
    this.generateGeneratedAnnotation();
    this.println("public class %s {", new Object[] {builderName});
    this.println();
    this.println("  private %s() {}", new Object[] {builderName});
    this.println("}");
  }
}
