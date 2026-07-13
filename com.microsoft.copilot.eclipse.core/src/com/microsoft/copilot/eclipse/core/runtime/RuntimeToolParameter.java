// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.runtime;

import java.util.Objects;

/**
 * Describes a single input parameter of a {@link RuntimeToolDescriptor}. A runtime provider uses these to declare the
 * schema of the tools it contributes, so Copilot can build the language-model tool schema generically without knowing
 * anything about the provider's domain.
 */
public class RuntimeToolParameter {

  /** Parameter type: a plain string value. */
  public static final String TYPE_STRING = "string";
  /** Parameter type: a boolean value. */
  public static final String TYPE_BOOLEAN = "boolean";
  /** Parameter type: a numeric value. */
  public static final String TYPE_NUMBER = "number";

  private final String name;
  private final String type;
  private final String description;
  private final boolean required;

  /**
   * Creates a tool parameter descriptor.
   *
   * @param name the parameter name as it appears in the tool arguments
   * @param type the JSON schema type, one of {@link #TYPE_STRING}, {@link #TYPE_BOOLEAN} or {@link #TYPE_NUMBER}
   * @param description a human/model-readable description of the parameter
   * @param required whether the parameter is required
   */
  public RuntimeToolParameter(String name, String type, String description, boolean required) {
    this.name = name;
    this.type = type != null ? type : TYPE_STRING;
    this.description = description;
    this.required = required;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public boolean isRequired() {
    return required;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, description, required);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    RuntimeToolParameter other = (RuntimeToolParameter) obj;
    return required == other.required && Objects.equals(name, other.name) && Objects.equals(type, other.type)
        && Objects.equals(description, other.description);
  }

  @Override
  public String toString() {
    return "RuntimeToolParameter[name=" + name + ", type=" + type + ", required=" + required + "]";
  }
}
