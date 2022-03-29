package com.yichi.onepiece;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class TestUtils {
  private TestUtils() {}

  public static void assertOk(ResponseEntity<?> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  public static <T> T assertOkAndGet(ResponseEntity<T> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody();
  }

  public static File getResourceFile(String path) {
    ClassLoader classLoader = TestUtils.class.getClassLoader();
    URL resource = Objects.requireNonNull(classLoader.getResource(path));
    return new File(resource.getFile());
  }

  public static String loadResourceFile(String path) throws IOException {
    return Files.readString(getResourceFile(path).toPath());
  }

  public static byte[] loadResourceFileInBytes(String path) throws IOException {
    return Files.readAllBytes(getResourceFile(path).toPath());
  }

  public static String getResourceFilePath(String path) {
    ClassLoader classLoader = TestUtils.class.getClassLoader();
    URL resource = Objects.requireNonNull(classLoader.getResource(path));
    return resource.getPath();
  }
}
