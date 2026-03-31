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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.strands.agent.Agent;
import org.springframework.ai.strands.agent.AgentResult;
import org.springframework.ai.strands.agent.AgentState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SwarmOrchestrator}.
 *
 * @author Spring AI
 */
class SwarmOrchestratorTests {

	@Test
	void basicSwarmWithCoordinatorAndSpecialists() {
		Agent coordinator = mockAgent("Coordinator output");
		Agent specialist1 = mockAgent("Specialist 1 output");
		Agent specialist2 = mockAgent("Specialist 2 output");

		SwarmOrchestrator swarm = new SwarmOrchestrator(coordinator, List.of(specialist1, specialist2), 1);

		MultiAgentResult result = swarm.invoke("test task");

		assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
		assertThat(result.getResults()).hasSize(3);
		assertThat(result.getResults()).containsKeys("coordinator-round-0", "specialist-0-round-0",
				"specialist-1-round-0");
	}

	@Test
	void maxRoundsLimit() {
		Agent coordinator = mockAgent("Coordinator output");
		Agent specialist = mockAgent("Specialist output");

		SwarmOrchestrator swarm = new SwarmOrchestrator(coordinator, List.of(specialist), 3);

		MultiAgentResult result = swarm.invoke("task");

		assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
		assertThat(result.getResults()).hasSize(6);
		assertThat(result.getResults()).containsKeys("coordinator-round-0", "specialist-0-round-0",
				"coordinator-round-1", "specialist-0-round-1", "coordinator-round-2", "specialist-0-round-2");
	}

	@Test
	void singleSpecialist() {
		Agent coordinator = mockAgent("Coordinator says hello");
		Agent specialist = mockAgent("Specialist result");

		SwarmOrchestrator swarm = new SwarmOrchestrator(coordinator, List.of(specialist), 1);

		MultiAgentResult result = swarm.invoke("simple task");

		assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
		assertThat(result.getResults()).hasSize(2);
		assertThat(result.getExecutionCount()).isEqualTo(2);
	}

	@Test
	void resultAccumulation() {
		Agent coordinator = mockAgent("Coord result");
		Agent specialist1 = mockAgent("Spec1 result");
		Agent specialist2 = mockAgent("Spec2 result");

		SwarmOrchestrator swarm = new SwarmOrchestrator(coordinator, List.of(specialist1, specialist2), 2);

		MultiAgentResult result = swarm.invoke("accumulation test");

		assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
		assertThat(result.getResults()).hasSize(6);
		assertThat(result.getExecutionCount()).isEqualTo(6);
		assertThat(result.getExecutionTimeMs()).isGreaterThanOrEqualTo(0);
	}

	@Test
	void swarmWithEmptyTask() {
		Agent coordinator = mockAgent("Coordinator response");
		Agent specialist = mockAgent("Specialist response");

		SwarmOrchestrator swarm = new SwarmOrchestrator(coordinator, List.of(specialist), 1);

		MultiAgentResult result = swarm.invoke("");

		assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
		assertThat(result.getResults()).isNotEmpty();
	}

	@SuppressWarnings("unchecked")
	private Agent mockAgent(String responseText) {
		Agent agent = mock(Agent.class);
		AgentResult agentResult = createAgentResult(responseText);
		when(agent.call(anyString(), anyMap())).thenReturn(agentResult);
		return agent;
	}

	private AgentResult createAgentResult(String text) {
		AssistantMessage message = new AssistantMessage(text);
		AgentState state = new AgentState();
		Map<String, Object> metrics = new HashMap<>();
		return new AgentResult(message, "end_turn", state, metrics);
	}

}
