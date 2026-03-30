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

package org.springframework.ai.strands.agent;

import java.util.Collections;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.util.Assert;

/**
 * Result of an {@link Agent} invocation.
 *
 * <p>
 * Contains the final assistant response, the stop reason, the agent state at completion,
 * and optional metrics such as token usage and timing information.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class AgentResult {

	private final AssistantMessage message;

	private final @Nullable String stopReason;

	private final AgentState state;

	private final Map<String, Object> metrics;

	/**
	 * Create a new {@code AgentResult}.
	 * @param message the final assistant message, must not be {@code null}
	 * @param stopReason the reason the model stopped generating, may be {@code null}
	 * @param state the agent state at completion, must not be {@code null}
	 * @param metrics the metrics map, must not be {@code null}
	 */
	public AgentResult(AssistantMessage message, @Nullable String stopReason, AgentState state,
			Map<String, Object> metrics) {
		Assert.notNull(message, "message must not be null");
		Assert.notNull(state, "state must not be null");
		Assert.notNull(metrics, "metrics must not be null");
		this.message = message;
		this.stopReason = stopReason;
		this.state = state;
		this.metrics = Collections.unmodifiableMap(metrics);
	}

	/**
	 * Return the final assistant message.
	 * @return the assistant message, never {@code null}
	 */
	public AssistantMessage getMessage() {
		return this.message;
	}

	/**
	 * Return the reason the model stopped generating.
	 * @return the stop reason, such as {@code "end_turn"}, {@code "tool_use"}, or
	 * {@code "max_tokens"}, or {@code null}
	 */
	public @Nullable String getStopReason() {
		return this.stopReason;
	}

	/**
	 * Return the agent state at the time of completion.
	 * @return the agent state
	 */
	public AgentState getState() {
		return this.state;
	}

	/**
	 * Return the metrics collected during the invocation.
	 * @return an unmodifiable map of metrics such as token usage and timing
	 */
	public Map<String, Object> getMetrics() {
		return this.metrics;
	}

}
