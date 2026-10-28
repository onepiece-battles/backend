package com.yichi.onepiece.Application;

import static org.assertj.core.api.Assertions.assertThat;

import com.yichi.onepiece.OnePieceApplication;
import com.yichi.onepiece.assistant.agent.AssistantToolProvider;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient;
import com.yichi.onepiece.user.model.FriendshipDao;
import com.yichi.onepiece.user.model.UserDao;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.mapper.ClassPathMapperScanner;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.testng.annotations.Test;

/**
 * Runs the application's {@link MapperScan} over the classpath without starting Spring. A
 * non-DAO interface picked up as a mapper becomes a bean of that type that throws on every call:
 * ToolRegistry received one for AssistantToolProvider and the application failed to start.
 */
public class MapperScanTests {
  @Test
  public void onlyDaosBecomeMappers() {
    Set<String> mappers = scannedMapperInterfaces();

    assertThat(mappers).contains(UserDao.class.getName(), FriendshipDao.class.getName());
    assertThat(mappers)
        .doesNotContain(AssistantToolProvider.class.getName(), ChatCompletionClient.class.getName());
    assertThat(mappers).allMatch(name -> name.endsWith("Dao") || name.endsWith("Mapper"));
  }

  private static Set<String> scannedMapperInterfaces() {
    MapperScan scan = OnePieceApplication.class.getAnnotation(MapperScan.class);
    ClassPathMapperScanner scanner = new ClassPathMapperScanner(new SimpleBeanDefinitionRegistry());
    if (scan.annotationClass() != Annotation.class) {
      scanner.setAnnotationClass(scan.annotationClass());
    }
    if (scan.markerInterface() != Class.class) {
      scanner.setMarkerInterface(scan.markerInterface());
    }
    scanner.registerFilters();
    return Stream.concat(Stream.of(scan.value()), Stream.of(scan.basePackages()))
        .flatMap(pkg -> scanner.findCandidateComponents(pkg).stream())
        .map(BeanDefinition::getBeanClassName)
        .collect(Collectors.toSet());
  }
}
