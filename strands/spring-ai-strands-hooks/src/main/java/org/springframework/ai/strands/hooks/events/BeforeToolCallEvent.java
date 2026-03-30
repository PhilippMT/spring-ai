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

package org.springframework.ai.strands.hooks.events;

import java.util.Collections;
import java.util.Map;

import org.springframework.ai.strands.hooks.HookEvent;
import org.springframework.util.Assert;

/**
 * Event fired before a tool is executed.
 *
 * <p>
 * The {@link #isCancelTool()} field is writable and can be set by callbacks to prevent
 * the tool from being executed.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class BeforeToolCallEvent extends HookEvent {

	private final String toolName;

	private final Map<String, Object> toolInput;

	private volatile boolean cancelTool;

	/**
	 * Create a new {@code BeforeToolCallEvent}.
	 * @param agentId the identifier of the agent
	 * @param toolName the name of the tool to be executed, must not be {@code null}
	 * @param toolInput the input parameters for the tool, must not be {@code null}
	 */
	public BeforeToolCallEvent(String agentId, String toolName, Map<String, Object> toolInput) {
		super(agentId);
		Assert.notNull(toolName, "toolName must not be null");
		Assert.notNull(toolInput, "toolInput must not be null");
		this.toolName = toolName;
		this.toolInput = Collections.unmodifiableMap(toolInput);
		this.cancelTool = false;
	}

	/**
	 * Return the name of the tool to be executed.
	 * @return the tool name, never {@code null}
	 */
	public String getToolName() {
		return this.toolName;
	}

	/**
	 * Return the input parameters for the tool.
	 * @return an unmodifiable view of the tool input
	 */
	public Map<String, Object> getToolInput() {
		return this.toolInput;
	}

	/**
	 * Return whether the tool execution should be cancelled.
	 * @return {@code true} if the tool should be cancelled, {@code false} otherwise
	 */
	public boolean isCancelTool() {
		return this.cancelTool;
	}

	/**
	 * Set whether the tool execution should be cancelled. This is a writable field that
	 * callbacks can modify.
	 * @param cancelTool {@code true} to cancel the tool execution
	 */
	public void setCancelTool(boolean cancelTool) {
		this.cancelTool = cancelTool;
	}

}
