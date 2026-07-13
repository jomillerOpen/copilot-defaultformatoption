// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.runtime;

import java.util.Objects;

/**
 * The result of invoking a {@link RuntimeToolDescriptor tool} on a runtime provider. The provider is responsible for
 * formatting {@link #getContent() content} into text suitable for a language model, so Copilot can relay it without
 * interpreting any provider-specific structure.
 */
public class RuntimeToolResult {

  private final boolean success;
  private final String content;

  /**
   * Creates a tool result.
   *
   * @param success whether the invocation succeeded
   * @param content the human/model-readable result or error message
   */
  public RuntimeToolResult(boolean success, String content) {
    this.success = success;
    this.content = content;
  }

  /**
   * Creates a successful result.
   *
   * @param content the result text
   * @return a successful {@link RuntimeToolResult}
   */
  public static RuntimeToolResult success(String content) {
    return new RuntimeToolResult(true, content);
  }

  /**
   * Creates an error result.
   *
   * @param content the error text
   * @return a failed {@link RuntimeToolResult}
   */
  public static RuntimeToolResult error(String content) {
    return new RuntimeToolResult(false, content);
  }

  public boolean isSuccess() {
    return success;
  }

  public String getContent() {
    return content;
  }

  @Override
  public int hashCode() {
    return Objects.hash(success, content);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    RuntimeToolResult other = (RuntimeToolResult) obj;
    return success == other.success && Objects.equals(content, other.content);
  }

  @Override
  public String toString() {
    return "RuntimeToolResult[success=" + success + "]";
  }
}
