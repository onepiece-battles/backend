package com.yichi.infra.support.mybatis.Entity;

import org.apache.ibatis.reflection.ReflectorFactory;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Add converter when creating reflector. This converter is needed when mybatis properties are
 * configured by application.properties file.
 *
 * @author: yichi
 */
@Component
@ConfigurationPropertiesBinding
public class ReflectorFactoryConverter implements Converter<String, ReflectorFactory> {
  @Override
  public ReflectorFactory convert(String source) {
    try {
      return (ReflectorFactory) Class.forName(source).getConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
