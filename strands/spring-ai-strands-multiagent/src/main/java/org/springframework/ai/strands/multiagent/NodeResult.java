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

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.strands.agent.AgentResult;
import org.springframework.util.Assert;

/**
 * Result from a single node execution within a multi-agent orchestration.
 *
 * <p>
 * A node result wraps the outcome of executing an agent or a nested multi-agent
 * orchestrator, along with accumulated usage metrics and execution timing.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class NodeResult {

	private final ResultValue result;

	private final long executionTimeMs;

	private final Status status;

	private final long inputTokens;

	private final long outputTokens;

	private final long totalTokens;

	private final long latencyMs;

	private final int executionCount;

	/**
	 * Create a new {@code NodeResult}.
	 * @param result the result value, must not be {@code null}
	 * @param executionTimeMs the execution time in milliseconds
	 * @param status the execution status, must not be {@code null}
	 * @param inputTokens the accumulated input token count
	 * @param outputTokens the accumulated output token count
	 * @param totalTokens the accumulated total token count
	 * @param latencyMs the accumulated latency in milliseconds
	 * @param executionCount the number of executions
	 */
	public NodeResult(ResultValue result, long executionTimeMs, Status status, long inputTokens, long outputTokens,
			long totalTokens, long latencyMs, int executionCount) {
		Assert.notNull(result, "result must not be null");
		Assert.notNull(status, "status must not be null");
		this.result = result;
		this.executionTimeMs = executionTimeMs;
		this.status = status;
		this.inputTokens = inputTokens;
		this.outputTokens = outputTokens;
		this.totalTokens = totalTokens;
		this.latencyMs = latencyMs;
		this.executionCount = executionCount;
	}

	/**
	 * Create a {@code NodeResult} from an {@link AgentResult}.
	 * @param agentResult the agent result
	 * @param executionTimeMs the execution time in milliseconds
	 * @return a new node result wrapping the agent result
	 */
	public static NodeResult fromAgentResult(AgentResult agentResult, long executionTimeMs) {
		return new NodeResult(new AgentResultValue(agentResult), executionTimeMs, Status.COMPLETED, 0, 0, 0,
				executionTimeMs, 1);
	}

	/**
	 * Create a {@code NodeResult} from an {@link Exception}.
	 * @param exception the exception
	 * @param executionTimeMs the execution time in milliseconds
	 * @return a new node result wrapping the exception
	 */
	public static NodeResult fromException(Exception exception, long executionTimeMs) {
		return new NodeResult(new ExceptionResultValue(exception), executionTimeMs, Status.FAILED, 0, 0, 0,
				executionTimeMs, 1);
	}

	/**
	 * Flatten nested results into a list of {@link AgentResult} instances.
	 * @return a list of agent results contained in this node result
	 */
	public List<AgentResult> getAgentResults() {
		List<AgentResult> results = new ArrayList<>();
		collectAgentResults(this.result, results);
		return results;
	}

	/**
	 * Return the result value.
	 * @return the result value
	 */
	public ResultValue getResult() {
		return this.result;
	}

	/**
	 * Return the execution time in milliseconds.
	 * @return the execution time
	 */
	public long getExecutionTimeMs() {
		return this.executionTimeMs;
	}

	/**
	 * Return the execution status.
	 * @return the status
	 */
	public Status getStatus() {
		return this.status;
	}

	/**
	 * Return the accumulated input token count.
	 * @return the input tokens
	 */
	public long getInputTokens() {
		return this.inputTokens;
	}

	/**
	 * Return the accumulated output token count.
	 * @return the output tokens
	 */
	public long getOutputTokens() {
		return this.outputTokens;
	}

	/**
	 * Return the accumulated total token count.
	 * @return the total tokens
	 */
	public long getTotalTokens() {
		return this.totalTokens;
	}

	/**
	 * Return the accumulated latency in milliseconds.
	 * @return the latency
	 */
	public long getLatencyMs() {
		return this.latencyMs;
	}

	/**
	 * Return the number of executions.
	 * @return the execution count
	 */
	public int getExecutionCount() {
		return this.executionCount;
	}

	private void collectAgentResults(ResultValue value, List<AgentResult> collected) {
		if (value instanceof AgentResultValue arv) {
			collected.add(arv.agentResult());
		}
		else if (value instanceof MultiAgentResultValue marv) {
			for (NodeResult nodeResult : marv.multiAgentResult().getResults().values()) {
				collectAgentResults(nodeResult.getResult(), collected);
			}
		}
	}

	/**
	 * Sealed interface representing the different result types a node can produce.
	 */
	public sealed interface ResultValue permits AgentResultValue, MultiAgentResultValue, ExceptionResultValue {

	}

	/**
	 * A node result containing an {@link AgentResult}.
	 *
	 * @param agentResult the agent result
	 */
	public record AgentResultValue(AgentResult agentResult) implements ResultValue {
	}

	/**
	 * A node result containing a {@link MultiAgentResult}.
	 *
	 * @param multiAgentResult the multi-agent result
	 */
	public record MultiAgentResultValue(MultiAgentResult multiAgentResult) implements ResultValue {
	}

	/**
	 * A node result containing an {@link Exception}.
	 *
	 * @param exception the exception that occurred during execution
	 */
	public record ExceptionResultValue(Exception exception) implements ResultValue {
	}

}
