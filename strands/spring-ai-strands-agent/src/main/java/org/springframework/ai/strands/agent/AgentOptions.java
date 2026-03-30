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
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Configuration options for an {@link Agent}.
 *
 * <p>
 * Use the {@link #builder()} method to create instances with a fluent API.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public final class AgentOptions {

	private static final int DEFAULT_MAX_ITERATIONS = 25;

	private final @Nullable String systemPrompt;

	private final int maxIterations;

	private final boolean recordConversation;

	private final Map<String, Object> invocationState;

	private AgentOptions(Builder builder) {
		this.systemPrompt = builder.systemPrompt;
		this.maxIterations = builder.maxIterations;
		this.recordConversation = builder.recordConversation;
		this.invocationState = Collections.unmodifiableMap(new HashMap<>(builder.invocationState));
	}

	/**
	 * Return the system prompt to prepend to conversations.
	 * @return the system prompt, or {@code null} if not set
	 */
	public @Nullable String getSystemPrompt() {
		return this.systemPrompt;
	}

	/**
	 * Return the maximum number of iterations the agent event loop will execute.
	 * @return the maximum iteration count
	 */
	public int getMaxIterations() {
		return this.maxIterations;
	}

	/**
	 * Return whether the agent should record the conversation history.
	 * @return {@code true} if conversation recording is enabled
	 */
	public boolean isRecordConversation() {
		return this.recordConversation;
	}

	/**
	 * Return the invocation state map passed through the agent lifecycle.
	 * @return an unmodifiable view of the invocation state
	 */
	public Map<String, Object> getInvocationState() {
		return this.invocationState;
	}

	/**
	 * Create a new {@link Builder}.
	 * @return a new builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for {@link AgentOptions}.
	 */
	public static final class Builder {

		private @Nullable String systemPrompt;

		private int maxIterations = DEFAULT_MAX_ITERATIONS;

		private boolean recordConversation = true;

		private Map<String, Object> invocationState = new HashMap<>();

		private Builder() {
		}

		/**
		 * Set the system prompt.
		 * @param systemPrompt the system prompt
		 * @return this builder
		 */
		public Builder systemPrompt(@Nullable String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		/**
		 * Set the maximum number of event loop iterations.
		 * @param maxIterations the maximum iterations, must be positive
		 * @return this builder
		 */
		public Builder maxIterations(int maxIterations) {
			Assert.isTrue(maxIterations > 0, "maxIterations must be positive");
			this.maxIterations = maxIterations;
			return this;
		}

		/**
		 * Set whether to record the conversation history.
		 * @param recordConversation {@code true} to record
		 * @return this builder
		 */
		public Builder recordConversation(boolean recordConversation) {
			this.recordConversation = recordConversation;
			return this;
		}

		/**
		 * Set the invocation state.
		 * @param invocationState the invocation state map, must not be {@code null}
		 * @return this builder
		 */
		public Builder invocationState(Map<String, Object> invocationState) {
			Assert.notNull(invocationState, "invocationState must not be null");
			this.invocationState = new HashMap<>(invocationState);
			return this;
		}

		/**
		 * Build the {@link AgentOptions}.
		 * @return a new {@code AgentOptions} instance
		 */
		public AgentOptions build() {
			return new AgentOptions(this);
		}

	}

}
