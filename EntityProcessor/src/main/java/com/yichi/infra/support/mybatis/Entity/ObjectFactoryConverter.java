package com.yichi.infra.support.mybatis.Entity;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Add converter when creating object. This converter is needed when mybatis properties are
 * configured by application.properties file.
 *
 * @author: yichi
 */
@Component
@ConfigurationPropertiesBinding
public class ObjectFactoryConverter implements Converter<String, ObjectFactory> {

  @Override
  public ObjectFactory convert(String source) {
    try {
      return (ObjectFactory) Class.forName(source).getConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
