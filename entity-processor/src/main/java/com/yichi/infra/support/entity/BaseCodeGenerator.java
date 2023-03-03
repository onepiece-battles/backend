package com.yichi.infra.support.entity;

import com.google.common.base.Strings;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.io.PrintWriter;
import java.io.Writer;
import javax.annotation.processing.ProcessingEnvironment;

public abstract class BaseCodeGenerator {
  private PrintWriter out;
  protected final ProcessingEnvironment processingEnvironment;

  BaseCodeGenerator(ProcessingEnvironment processingEnvironment, Writer writer) {
    this.processingEnvironment = processingEnvironment;
    this.out = new PrintWriter(writer);
  }

  @FormatMethod
  public void println(@FormatString String format, Object... args) {
    this.out.println(String.format(format, args));
  }

  public void println() {
    this.out.println();
  }

  public void println(String content) {
    this.out.println(content);
  }

  @FormatMethod
  public void print(@FormatString String format, Object... args) {
    this.out.print(String.format(format, args));
  }

  public void print(String content) {
    this.out.print(content);
  }

  public void printJavadoc(String javadoc) {
    if (!Strings.isNullOrEmpty(javadoc)) {
      this.out.println("  /**");
      this.out.print("   * ");
      this.out.println(javadoc.trim().replace("\n", "\n   *"));
      this.out.println("   */");
    }
  }

  public void generateGeneratedAnnotation() {
    this.println(
        "@javax.annotation.processing.Generated(\"com.yichi.infra.support.entity.EntityProcessor\")");
  }
}
