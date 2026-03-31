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

import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.observation.ObservationRegistry;

import org.springframework.util.Assert;

/**
 * Simple metrics collector that tracks agent invocations, tool calls, errors, and token
 * usage using Micrometer.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class MetricsCollector {

	private final ObservationRegistry observationRegistry;

	private final AtomicLong invocationCount = new AtomicLong();

	private final AtomicLong toolCallCount = new AtomicLong();

	private final AtomicLong errorCount = new AtomicLong();

	private final AtomicLong totalInputTokens = new AtomicLong();

	private final AtomicLong totalOutputTokens = new AtomicLong();

	public MetricsCollector(ObservationRegistry observationRegistry) {
		Assert.notNull(observationRegistry, "observationRegistry must not be null");
		this.observationRegistry = observationRegistry;
	}

	/**
	 * Record an agent invocation.
	 * @param agentId the agent identifier
	 * @param durationMs the invocation duration in milliseconds
	 */
	public void recordInvocation(String agentId, long durationMs) {
		Assert.hasText(agentId, "agentId must not be null or empty");
		this.invocationCount.incrementAndGet();
	}

	/**
	 * Record a tool call.
	 * @param toolName the name of the tool
	 * @param durationMs the tool call duration in milliseconds
	 * @param success whether the tool call was successful
	 */
	public void recordToolCall(String toolName, long durationMs, boolean success) {
		Assert.hasText(toolName, "toolName must not be null or empty");
		this.toolCallCount.incrementAndGet();
		if (!success) {
			this.errorCount.incrementAndGet();
		}
	}

	/**
	 * Record token usage for an agent invocation.
	 * @param agentId the agent identifier
	 * @param inputTokens the number of input tokens
	 * @param outputTokens the number of output tokens
	 */
	public void recordTokenUsage(String agentId, long inputTokens, long outputTokens) {
		Assert.hasText(agentId, "agentId must not be null or empty");
		this.totalInputTokens.addAndGet(inputTokens);
		this.totalOutputTokens.addAndGet(outputTokens);
	}

	public long getInvocationCount() {
		return this.invocationCount.get();
	}

	public long getToolCallCount() {
		return this.toolCallCount.get();
	}

	public long getErrorCount() {
		return this.errorCount.get();
	}

	public long getTotalInputTokens() {
		return this.totalInputTokens.get();
	}

	public long getTotalOutputTokens() {
		return this.totalOutputTokens.get();
	}

	public ObservationRegistry getObservationRegistry() {
		return this.observationRegistry;
	}

}
