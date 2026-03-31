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

package org.springframework.ai.strands.multiagent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Result from a multi-agent orchestration execution.
 *
 * <p>
 * Aggregates individual {@link NodeResult} instances from all executed nodes along with
 * accumulated usage metrics and timing information.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class MultiAgentResult {

	private final Status status;

	private final Map<String, NodeResult> results;

	private final long accumulatedInputTokens;

	private final long accumulatedOutputTokens;

	private final long accumulatedTotalTokens;

	private final long accumulatedLatencyMs;

	private final int executionCount;

	private final long executionTimeMs;

	/**
	 * Create a new {@code MultiAgentResult}.
	 * @param status the overall execution status, must not be {@code null}
	 * @param results the node results keyed by node identifier, must not be {@code null}
	 * @param accumulatedInputTokens the accumulated input token count
	 * @param accumulatedOutputTokens the accumulated output token count
	 * @param accumulatedTotalTokens the accumulated total token count
	 * @param accumulatedLatencyMs the accumulated latency in milliseconds
	 * @param executionCount the total number of node executions
	 * @param executionTimeMs the total execution time in milliseconds
	 */
	public MultiAgentResult(Status status, Map<String, NodeResult> results, long accumulatedInputTokens,
			long accumulatedOutputTokens, long accumulatedTotalTokens, long accumulatedLatencyMs, int executionCount,
			long executionTimeMs) {
		Assert.notNull(status, "status must not be null");
		Assert.notNull(results, "results must not be null");
		this.status = status;
		this.results = Collections.unmodifiableMap(new LinkedHashMap<>(results));
		this.accumulatedInputTokens = accumulatedInputTokens;
		this.accumulatedOutputTokens = accumulatedOutputTokens;
		this.accumulatedTotalTokens = accumulatedTotalTokens;
		this.accumulatedLatencyMs = accumulatedLatencyMs;
		this.executionCount = executionCount;
		this.executionTimeMs = executionTimeMs;
	}

	/**
	 * Return the overall execution status.
	 * @return the status
	 */
	public Status getStatus() {
		return this.status;
	}

	/**
	 * Return the node results keyed by node identifier.
	 * @return an unmodifiable map of node results
	 */
	public Map<String, NodeResult> getResults() {
		return this.results;
	}

	/**
	 * Return the accumulated input token count across all nodes.
	 * @return the accumulated input tokens
	 */
	public long getAccumulatedInputTokens() {
		return this.accumulatedInputTokens;
	}

	/**
	 * Return the accumulated output token count across all nodes.
	 * @return the accumulated output tokens
	 */
	public long getAccumulatedOutputTokens() {
		return this.accumulatedOutputTokens;
	}

	/**
	 * Return the accumulated total token count across all nodes.
	 * @return the accumulated total tokens
	 */
	public long getAccumulatedTotalTokens() {
		return this.accumulatedTotalTokens;
	}

	/**
	 * Return the accumulated latency in milliseconds across all nodes.
	 * @return the accumulated latency
	 */
	public long getAccumulatedLatencyMs() {
		return this.accumulatedLatencyMs;
	}

	/**
	 * Return the total number of node executions.
	 * @return the execution count
	 */
	public int getExecutionCount() {
		return this.executionCount;
	}

	/**
	 * Return the total execution time in milliseconds.
	 * @return the execution time
	 */
	public long getExecutionTimeMs() {
		return this.executionTimeMs;
	}

}
