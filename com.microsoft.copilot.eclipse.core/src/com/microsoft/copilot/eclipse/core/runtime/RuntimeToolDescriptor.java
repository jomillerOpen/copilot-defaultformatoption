// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Describes a single tool that a runtime provider contributes to Copilot. The provider owns the tool's name,
 * description and input schema, so Copilot can expose it to the language model generically without knowing anything
 * about the provider's domain (for example whether the runtime is Content Server, a database, or something else).
 */
public class RuntimeToolDescriptor {

  private final String name;
  private final String description;
  private final List<RuntimeToolParameter> parameters;

  /**
   * Creates a tool descriptor.
   *
   * @param name the unique tool name exposed to the language model
   * @param description a description telling the model what the tool does and when to use it
   * @param parameters the tool's input parameters; may be {@code null} or empty
   */
  public RuntimeToolDescriptor(String name, String description, List<RuntimeToolParameter> parameters) {
    this.name = name;
    this.description = description;
    this.parameters = parameters != null ? new ArrayList<>(parameters) : new ArrayList<>();
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Returns the tool's parameters.
   *
   * @return an unmodifiable list of parameters, never {@code null}
   */
  public List<RuntimeToolParameter> getParameters() {
    return Collections.unmodifiableList(parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, parameters);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    RuntimeToolDescriptor other = (RuntimeToolDescriptor) obj;
    return Objects.equals(name, other.name) && Objects.equals(description, other.description)
        && Objects.equals(parameters, other.parameters);
  }

  @Override
  public String toString() {
    return "RuntimeToolDescriptor[name=" + name + ", parameters=" + parameters.size() + "]";
  }
}
