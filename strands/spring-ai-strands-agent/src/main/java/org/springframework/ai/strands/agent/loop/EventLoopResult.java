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

package org.springframework.ai.strands.agent.loop;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.util.Assert;

/**
 * Result of the agent event loop execution.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class EventLoopResult {

	private final AssistantMessage message;

	private final @Nullable String stopReason;

	private final boolean requiresToolExecution;

	private final List<ToolCallResult> toolResults;

	/**
	 * Create a new {@code EventLoopResult}.
	 * @param message the final assistant message, must not be {@code null}
	 * @param stopReason the reason the model stopped generating, may be {@code null}
	 * @param requiresToolExecution whether the result still requires tool execution
	 * @param toolResults the tool call results from the loop, must not be {@code null}
	 */
	public EventLoopResult(AssistantMessage message, @Nullable String stopReason, boolean requiresToolExecution,
			List<ToolCallResult> toolResults) {
		Assert.notNull(message, "message must not be null");
		Assert.notNull(toolResults, "toolResults must not be null");
		this.message = message;
		this.stopReason = stopReason;
		this.requiresToolExecution = requiresToolExecution;
		this.toolResults = Collections.unmodifiableList(toolResults);
	}

	/**
	 * Return the final assistant message.
	 * @return the assistant message
	 */
	public AssistantMessage getMessage() {
		return this.message;
	}

	/**
	 * Return the reason the model stopped generating.
	 * @return the stop reason, may be {@code null}
	 */
	public @Nullable String getStopReason() {
		return this.stopReason;
	}

	/**
	 * Return whether the result still requires tool execution. This is {@code false} for
	 * the final result of a completed agent loop.
	 * @return {@code true} if tool execution is still pending
	 */
	public boolean isRequiresToolExecution() {
		return this.requiresToolExecution;
	}

	/**
	 * Return the tool call results collected during the loop.
	 * @return an unmodifiable list of tool call results
	 */
	public List<ToolCallResult> getToolResults() {
		return this.toolResults;
	}

	/**
	 * Result of an individual tool call execution.
	 *
	 * @param toolName the name of the tool that was called
	 * @param result the string result returned by the tool
	 */
	public record ToolCallResult(String toolName, String result) {
	}

}
