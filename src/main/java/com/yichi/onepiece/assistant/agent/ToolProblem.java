package com.yichi.onepiece.assistant.agent;

/** An actionable tool error (bad arguments, ambiguous name, stale draft). */
public final class ToolProblem extends RuntimeException {
  private final ToolResult result;

  public ToolProblem(String message, ToolResult result) {
    super(message);
    this.result = result;
  }

  public ToolResult result() {
    return result;
  }
}
