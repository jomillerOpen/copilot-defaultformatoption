// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.microsoft.copilot.eclipse.core.CopilotCore;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchema;
import com.microsoft.copilot.eclipse.core.lsp.protocol.InputSchemaPropertyValue;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolInformation;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult;
import com.microsoft.copilot.eclipse.core.lsp.protocol.LanguageModelToolResult.ToolInvocationStatus;
import com.microsoft.copilot.eclipse.core.runtime.IRuntimeInspectionService;
import com.microsoft.copilot.eclipse.core.runtime.RuntimeToolDescriptor;
import com.microsoft.copilot.eclipse.core.runtime.RuntimeToolParameter;
import com.microsoft.copilot.eclipse.core.runtime.RuntimeToolResult;
import com.microsoft.copilot.eclipse.ui.chat.ChatView;

/**
 * Generic agent tool that exposes a single tool declared by a runtime provider (an {@link IRuntimeInspectionService})
 * to the language model.
 *
 * <p>This class contains no runtime-specific knowledge: it builds the tool schema from the provider's
 * {@link RuntimeToolDescriptor} and forwards invocations to {@link IRuntimeInspectionService#invokeTool}. The provider
 * owns the tool's name, description, parameters and result formatting, so any runtime can contribute tools without
 * changes to Copilot.</p>
 */
public class RuntimeProviderTool extends BaseTool {

  private final IRuntimeInspectionService service;
  private final RuntimeToolDescriptor descriptor;

  /**
   * Creates a tool that bridges a single provider-declared tool.
   *
   * @param service the provider that owns the tool
   * @param descriptor the tool the provider declared
   */
  public RuntimeProviderTool(IRuntimeInspectionService service, RuntimeToolDescriptor descriptor) {
    this.service = service;
    this.descriptor = descriptor;
    this.name = descriptor.getName();
  }

  @Override
  public LanguageModelToolInformation getToolInformation() {
    LanguageModelToolInformation toolInfo = super.getToolInformation();
    toolInfo.setName(descriptor.getName());
    toolInfo.setDescription(descriptor.getDescription());

    InputSchema inputSchema = new InputSchema();
    inputSchema.setType("object");

    Map<String, InputSchemaPropertyValue> properties = new HashMap<>();
    List<String> required = new java.util.ArrayList<>();
    for (RuntimeToolParameter parameter : descriptor.getParameters()) {
      properties.put(parameter.getName(),
          new InputSchemaPropertyValue(parameter.getType(), parameter.getDescription()));
      if (parameter.isRequired()) {
        required.add(parameter.getName());
      }
    }
    inputSchema.setProperties(properties);
    inputSchema.setRequired(required);
    toolInfo.setInputSchema(inputSchema);

    return toolInfo;
  }

  @Override
  public CompletableFuture<LanguageModelToolResult[]> invoke(Map<String, Object> input, ChatView chatView) {
    if (!service.isAvailable()) {
      LanguageModelToolResult result = new LanguageModelToolResult();
      result.setStatus(ToolInvocationStatus.error);
      result.addContent("The '" + safeProviderName() + "' runtime is not currently available. Ensure the provider is "
          + "connected and its runtime is running, then try again.");
      return CompletableFuture.completedFuture(new LanguageModelToolResult[] { result });
    }

    Map<String, Object> arguments = input != null ? input : Map.of();
    return CompletableFuture
        .supplyAsync(() -> service.invokeTool(descriptor.getName(), arguments, new NullProgressMonitor()))
        .thenApply(this::toToolResults)
        .exceptionally(throwable -> {
          CopilotCore.LOGGER.error("Runtime tool '" + descriptor.getName() + "' failed", throwable);
          LanguageModelToolResult result = new LanguageModelToolResult();
          result.setStatus(ToolInvocationStatus.error);
          result.addContent("Failed to invoke the runtime tool: " + throwable.getMessage());
          return new LanguageModelToolResult[] { result };
        });
  }

  private LanguageModelToolResult[] toToolResults(RuntimeToolResult providerResult) {
    LanguageModelToolResult result = new LanguageModelToolResult();
    if (providerResult == null) {
      result.setStatus(ToolInvocationStatus.error);
      result.addContent("The runtime tool returned no result.");
      return new LanguageModelToolResult[] { result };
    }
    String content = providerResult.getContent();
    result.addContent(StringUtils.isNotBlank(content) ? content : "The runtime tool returned no content.");
    result.setStatus(providerResult.isSuccess() ? ToolInvocationStatus.success : ToolInvocationStatus.error);
    return new LanguageModelToolResult[] { result };
  }

  private String safeProviderName() {
    try {
      String providerName = service.getProviderName();
      return StringUtils.isNotBlank(providerName) ? providerName : "runtime";
    } catch (RuntimeException e) {
      return "runtime";
    }
  }
}
