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

package org.springframework.ai.strands.telemetry;

import io.micrometer.observation.Observation;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Observation context carrying agent invocation data.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class AgentObservationContext extends Observation.Context {

	private final String agentId;

	private @Nullable String systemPrompt;

	private @Nullable String stopReason;

	private long inputTokens;

	private long outputTokens;

	public AgentObservationContext(String agentId) {
		Assert.hasText(agentId, "agentId must not be null or empty");
		this.agentId = agentId;
	}

	public String getAgentId() {
		return this.agentId;
	}

	@Nullable public String getSystemPrompt() {
		return this.systemPrompt;
	}

	public void setSystemPrompt(@Nullable String systemPrompt) {
		this.systemPrompt = systemPrompt;
	}

	@Nullable public String getStopReason() {
		return this.stopReason;
	}

	public void setStopReason(@Nullable String stopReason) {
		this.stopReason = stopReason;
	}

	public long getInputTokens() {
		return this.inputTokens;
	}

	public void setInputTokens(long inputTokens) {
		this.inputTokens = inputTokens;
	}

	public long getOutputTokens() {
		return this.outputTokens;
	}

	public void setOutputTokens(long outputTokens) {
		this.outputTokens = outputTokens;
	}

}
