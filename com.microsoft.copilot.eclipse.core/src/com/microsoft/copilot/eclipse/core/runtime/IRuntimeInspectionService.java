// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.core.runtime;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Service Provider Interface that lets a third-party plug-in contribute tools which inspect a live language runtime to
 * Copilot's agent.
 *
 * <p>Copilot is deliberately agnostic about what the runtime is: a provider (for example the OpenText Content Server
 * development plug-in, CSIDE) registers an implementation of this interface as an OSGi service, <em>declares its own
 * tools</em> via {@link #getToolDescriptors()}, and executes them in {@link #invokeTool}. Copilot merely discovers the
 * provider through {@link RuntimeInspectionServiceManager}, exposes each declared tool to the language model, and
 * relays the textual result. As a result, no runtime-specific vocabulary lives in Copilot &ndash; the provider owns
 * the tool names, descriptions, parameter schemas and result formatting.</p>
 */
public interface IRuntimeInspectionService {

  /**
   * Returns a short, human-readable name for the provider/runtime, for example {@code "Content Server"}. It may be
   * surfaced to the user in diagnostics.
   *
   * @return the provider display name
   */
  String getProviderName();

  /**
   * Indicates whether the runtime is currently available so its tools can run. A provider may be registered even when
   * its runtime (for example a server VM) is not running; in that case this returns {@code false} and Copilot will
   * report the runtime as unavailable rather than invoking a tool.
   *
   * @return {@code true} if the runtime is loaded and its tools can be invoked
   */
  boolean isAvailable();

  /**
   * Returns the tools this provider contributes. Each descriptor carries the tool's name, description and input schema,
   * which Copilot uses verbatim to expose the tool to the language model.
   *
   * @return the tool descriptors, never {@code null}
   */
  List<RuntimeToolDescriptor> getToolDescriptors();

  /**
   * Invokes one of the provider's declared tools.
   *
   * <p>Implementations must not assume they are running on the UI thread and should honour cancellation via the
   * supplied monitor. The returned {@link RuntimeToolResult#getContent() content} must be text ready to hand to a
   * language model; the provider is responsible for all formatting and truncation.</p>
   *
   * @param toolName the name of the tool to invoke, matching a {@link RuntimeToolDescriptor#getName()}
   * @param arguments the tool arguments keyed by parameter name; never {@code null}
   * @param monitor a progress monitor used to report progress and observe cancellation; never {@code null}
   * @return the result of the invocation, never {@code null}
   */
  RuntimeToolResult invokeTool(String toolName, Map<String, Object> arguments, IProgressMonitor monitor);
}