package com.yichi.infra.support.entity.mapstruct;

import com.yichi.infra.support.entity.Entity;
import com.yichi.infra.support.entity.EntityUtil;
import java.util.Iterator;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import org.mapstruct.ap.spi.BuilderInfo;
import org.mapstruct.ap.spi.BuilderInfo.Builder;
import org.mapstruct.ap.spi.DefaultBuilderProvider;

public class EntityBuilderProvider extends DefaultBuilderProvider {
  public EntityBuilderProvider() {}

  public BuilderInfo findBuilderInfo(TypeElement element) {
    if (element.getAnnotation(Entity.class) != null) {
      String implName = EntityUtil.getImplClass(element.getQualifiedName().toString());
      TypeElement impl = this.elementUtils.getTypeElement(implName);
      if (impl == null) {
        String error =
            String.format(
                "Can not find implementation class of %s, see %s for further details.",
                element.getQualifiedName(),
                "https://yaocheng.feishu.cn/docs/doccnVYlgEtqzGvfoO4UxCRn4ug#58cNCR");
        System.err.println(error);
        throw new RuntimeException(error);
      }

      List<ExecutableElement> constructors =
          ElementFilter.constructorsIn(impl.getEnclosedElements());
      Iterator var5 = constructors.iterator();

      while (var5.hasNext()) {
        ExecutableElement constructor = (ExecutableElement) var5.next();
        if (constructor.getParameters().isEmpty()) {
          return (new Builder())
              .builderCreationMethod(constructor)
              .buildMethod(ElementFilter.methodsIn(impl.getEnclosedElements()))
              .build();
        }
      }
    }

    return null;
  }
}
