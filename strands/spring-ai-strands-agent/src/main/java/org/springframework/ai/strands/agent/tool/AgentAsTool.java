/*
 * Copyright 2023-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.strands.agent.tool;

import org.springframework.ai.strands.agent.Agent;
import org.springframework.ai.strands.agent.AgentResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.util.Assert;

/**
 * Adapter that wraps an {@link Agent} as a {@link ToolCallback} so it can be used as a
 * tool by another agent. This enables multi-agent composition where one agent delegates
 * sub-tasks to specialized agents.
 *
 * @author Spring AI
 * @since 2.0.0
 * @see Agent
 * @see ToolCallback
 */
public class AgentAsTool implements ToolCallback {

	private static final String INPUT_SCHEMA = "{\"type\":\"object\","
			+ "\"properties\":{\"input\":{\"type\":\"string\","
			+ "\"description\":\"The natural-language input to send to the agent\"}}," + "\"required\":[\"input\"]}";

	private final Agent agent;

	private final String name;

	private final String description;

	/**
	 * Create a new {@code AgentAsTool} wrapping the given agent.
	 * @param agent the agent to wrap, must not be {@code null}
	 * @param name the tool name, must not be {@code null}
	 * @param description a description of what this agent-tool does, must not be
	 * {@code null}
	 */
	public AgentAsTool(Agent agent, String name, String description) {
		Assert.notNull(agent, "agent must not be null");
		Assert.hasText(name, "name must not be null or empty");
		Assert.hasText(description, "description must not be null or empty");
		this.agent = agent;
		this.name = name;
		this.description = description;
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return ToolDefinition.builder().name(this.name).description(this.description).inputSchema(INPUT_SCHEMA).build();
	}

	@Override
	public String call(String toolInput) {
		try {
			String input = extractInput(toolInput);
			AgentResult result = this.agent.call(input);
			String text = result.getMessage().getText();
			return text != null ? text : "";
		}
		catch (Exception ex) {
			throw new ToolExecutionException(getToolDefinition(), ex);
		}
	}

	private String extractInput(String toolInput) {
		if (toolInput == null || toolInput.isBlank()) {
			return "";
		}
		try {
			com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(toolInput);
			if (node.has("input")) {
				return node.get("input").asText();
			}
			return toolInput;
		}
		catch (Exception ex) {
			return toolInput;
		}
	}

}
