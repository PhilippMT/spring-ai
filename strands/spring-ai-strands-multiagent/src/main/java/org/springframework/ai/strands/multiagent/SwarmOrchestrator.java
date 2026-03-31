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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.strands.agent.Agent;
import org.springframework.ai.strands.agent.AgentResult;
import org.springframework.util.Assert;

/**
 * Swarm-based collaborative multi-agent orchestrator.
 *
 * <p>
 * Implements a swarm pattern where a coordinator agent delegates work to specialist
 * agents. The coordinator and specialists share a working memory that accumulates results
 * across rounds.
 *
 * <p>
 * In each round the coordinator is called with the current task and accumulated context,
 * then each specialist agent processes the task with the shared working memory. Execution
 * continues for a configurable number of rounds.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class SwarmOrchestrator extends MultiAgentBase {

	private static final Logger logger = LoggerFactory.getLogger(SwarmOrchestrator.class);

	private final Agent coordinatorAgent;

	private final List<Agent> specialistAgents;

	private final int maxRounds;

	/**
	 * Create a new {@code SwarmOrchestrator}.
	 * @param coordinatorAgent the coordinator agent that orchestrates the specialists,
	 * must not be {@code null}
	 * @param specialistAgents the list of specialist agents, must not be {@code null} or
	 * empty
	 * @param maxRounds the maximum number of coordination rounds, must be at least 1
	 */
	public SwarmOrchestrator(Agent coordinatorAgent, List<Agent> specialistAgents, int maxRounds) {
		Assert.notNull(coordinatorAgent, "coordinatorAgent must not be null");
		Assert.notNull(specialistAgents, "specialistAgents must not be null");
		Assert.notEmpty(specialistAgents, "specialistAgents must not be empty");
		Assert.isTrue(maxRounds >= 1, "maxRounds must be at least 1");
		this.coordinatorAgent = coordinatorAgent;
		this.specialistAgents = List.copyOf(specialistAgents);
		this.maxRounds = maxRounds;
	}

	@Override
	protected MultiAgentResult invokeInternal(String task, Map<String, Object> invocationState) {
		long startTime = System.currentTimeMillis();
		Map<String, NodeResult> results = new LinkedHashMap<>();
		StringBuilder workingMemory = new StringBuilder();

		long totalInputTokens = 0;
		long totalOutputTokens = 0;
		long totalTokens = 0;
		long totalLatency = 0;
		int totalExecutionCount = 0;
		Status overallStatus = Status.COMPLETED;

		for (int round = 0; round < this.maxRounds; round++) {
			logger.debug("Swarm orchestrator {} starting round {}/{}", getId(), round + 1, this.maxRounds);

			String coordinatorInput = buildCoordinatorInput(task, workingMemory);
			long coordinatorStart = System.currentTimeMillis();
			try {
				AgentResult coordinatorResult = this.coordinatorAgent.call(coordinatorInput, invocationState);
				long coordinatorElapsed = System.currentTimeMillis() - coordinatorStart;
				String coordinatorKey = "coordinator-round-" + round;
				NodeResult coordinatorNodeResult = NodeResult.fromAgentResult(coordinatorResult, coordinatorElapsed);
				results.put(coordinatorKey, coordinatorNodeResult);
				totalLatency += coordinatorElapsed;
				totalExecutionCount++;

				String coordinatorOutput = coordinatorResult.getMessage().getText();
				workingMemory.append("[Coordinator round ").append(round).append("] ").append(coordinatorOutput);
				workingMemory.append("\n");
			}
			catch (Exception ex) {
				long coordinatorElapsed = System.currentTimeMillis() - coordinatorStart;
				String coordinatorKey = "coordinator-round-" + round;
				results.put(coordinatorKey, NodeResult.fromException(ex, coordinatorElapsed));
				overallStatus = Status.FAILED;
				totalLatency += coordinatorElapsed;
				break;
			}

			for (int i = 0; i < this.specialistAgents.size(); i++) {
				Agent specialist = this.specialistAgents.get(i);
				String specialistInput = buildSpecialistInput(task, workingMemory);
				long specialistStart = System.currentTimeMillis();
				try {
					AgentResult specialistResult = specialist.call(specialistInput, invocationState);
					long specialistElapsed = System.currentTimeMillis() - specialistStart;
					String specialistKey = "specialist-" + i + "-round-" + round;
					NodeResult specialistNodeResult = NodeResult.fromAgentResult(specialistResult, specialistElapsed);
					results.put(specialistKey, specialistNodeResult);
					totalLatency += specialistElapsed;
					totalExecutionCount++;

					String specialistOutput = specialistResult.getMessage().getText();
					workingMemory.append("[Specialist ")
						.append(i)
						.append(" round ")
						.append(round)
						.append("] ")
						.append(specialistOutput);
					workingMemory.append("\n");
				}
				catch (Exception ex) {
					long specialistElapsed = System.currentTimeMillis() - specialistStart;
					String specialistKey = "specialist-" + i + "-round-" + round;
					results.put(specialistKey, NodeResult.fromException(ex, specialistElapsed));
					overallStatus = Status.FAILED;
					totalLatency += specialistElapsed;
				}
			}
		}

		long executionTimeMs = System.currentTimeMillis() - startTime;
		return new MultiAgentResult(overallStatus, results, totalInputTokens, totalOutputTokens, totalTokens,
				totalLatency, totalExecutionCount, executionTimeMs);
	}

	private String buildCoordinatorInput(String task, StringBuilder workingMemory) {
		if (workingMemory.length() == 0) {
			return task;
		}
		return task + "\n\nPrevious results:\n" + workingMemory;
	}

	private String buildSpecialistInput(String task, StringBuilder workingMemory) {
		if (workingMemory.length() == 0) {
			return task;
		}
		return task + "\n\nContext:\n" + workingMemory;
	}

	/**
	 * Return the coordinator agent.
	 * @return the coordinator agent
	 */
	public Agent getCoordinatorAgent() {
		return this.coordinatorAgent;
	}

	/**
	 * Return the specialist agents.
	 * @return an unmodifiable list of specialist agents
	 */
	public List<Agent> getSpecialistAgents() {
		return this.specialistAgents;
	}

	/**
	 * Return the maximum number of coordination rounds.
	 * @return the max rounds
	 */
	public int getMaxRounds() {
		return this.maxRounds;
	}

}
