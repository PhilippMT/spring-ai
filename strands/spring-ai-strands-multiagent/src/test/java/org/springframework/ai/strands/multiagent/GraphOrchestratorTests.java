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
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.strands.agent.Agent;
import org.springframework.ai.strands.agent.AgentResult;
import org.springframework.ai.strands.agent.AgentState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GraphOrchestrator}.
 *
 * @author Spring AI
 */
class GraphOrchestratorTests {

	@Test
	void simpleLinearGraph() {
		Agent agentA = mockAgent("Result from A");
		Agent agentB = mockAgent("Result from B");
		Agent agentC = mockAgent("Result from C");

		GraphOrchestrator orchestrator = new GraphBuilder().addNode("A", agentA)
			.addNode("B", agentB)
			.addNode("C", agentC)
			.addEdge("A", "B")
			.addEdge("B", "C")
			.setEntryPoint("A")
			.build();

		MultiAgentResult result = orchestrator.invoke("test task");

		assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
		assertThat(result.getResults()).hasSize(3);
		assertThat(result.getResults()).containsKeys("A", "B", "C");
	}

	@Test
	void parallelNodes() {
		Agent agentA = mockAgent("Result from A");
		Agent agentB = mockAgent("Result from B");
		Agent agentC = mockAgent("Result from C");
		Agent agentD = mockAgent("Result from D");

		GraphOrchestrator orchestrator = new GraphBuilder().addNode("A", agentA)
			.addNode("B", agentB)
			.addNode("C", agentC)
			.addNode("D", agentD)
			.addEdge("A", "B")
			.addEdge("A", "C")
			.addEdge("B", "D")
			.addEdge("C", "D")
			.setEntryPoint("A")
			.build();

		MultiAgentResult result = orchestrator.invoke("test task");

		assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
		assertThat(result.getResults()).hasSize(4);
		assertThat(result.getResults()).containsKeys("A", "B", "C", "D");
	}

	@Test
	void singleNodeGraph() {
		Agent agent = mockAgent("Single result");

		GraphOrchestrator orchestrator = new GraphBuilder().addNode("only", agent).setEntryPoint("only").build();

		MultiAgentResult result = orchestrator.invoke("single task");

		assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
		assertThat(result.getResults()).hasSize(1);
		assertThat(result.getResults()).containsKey("only");
	}

	@Test
	void cycleDetection() {
		Agent agentA = mockAgent("A");
		Agent agentB = mockAgent("B");
		Agent agentC = mockAgent("C");

		assertThatThrownBy(() -> new GraphBuilder().addNode("A", agentA)
			.addNode("B", agentB)
			.addNode("C", agentC)
			.addEdge("A", "B")
			.addEdge("B", "C")
			.addEdge("C", "A")
			.setEntryPoint("A")
			.build()).isInstanceOf(IllegalStateException.class).hasMessageContaining("cycle");
	}

	@Test
	void entryPointValidation() {
		Agent agent = mockAgent("A");

		assertThatThrownBy(() -> new GraphBuilder().addNode("A", agent).setEntryPoint("nonexistent").build())
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void nodeResultAccumulation() {
		Agent agentA = mockAgent("Result A");
		Agent agentB = mockAgent("Result B");

		GraphOrchestrator orchestrator = new GraphBuilder().addNode("A", agentA)
			.addNode("B", agentB)
			.addEdge("A", "B")
			.setEntryPoint("A")
			.build();

		MultiAgentResult result = orchestrator.invoke("task");

		assertThat(result.getExecutionCount()).isEqualTo(2);
		assertThat(result.getExecutionTimeMs()).isGreaterThanOrEqualTo(0);

		for (NodeResult nodeResult : result.getResults().values()) {
			assertThat(nodeResult.getStatus()).isEqualTo(Status.COMPLETED);
			assertThat(nodeResult.getExecutionCount()).isEqualTo(1);
		}
	}

	@Test
	void outputFlowsFromSourceToTarget() {
		Agent agentA = mockAgent("output-from-A");
		Agent agentB = mock(Agent.class);
		when(agentB.call(anyString(), anyMap())).thenAnswer(invocation -> {
			String input = invocation.getArgument(0);
			return createAgentResult("received: " + input);
		});

		GraphOrchestrator orchestrator = new GraphBuilder().addNode("A", agentA)
			.addNode("B", agentB)
			.addEdge("A", "B")
			.setEntryPoint("A")
			.build();

		MultiAgentResult result = orchestrator.invoke("initial task");

		assertThat(result.getStatus()).isEqualTo(Status.COMPLETED);
		NodeResult nodeResultB = result.getResults().get("B");
		assertThat(nodeResultB).isNotNull();
		assertThat(nodeResultB.getAgentResults()).hasSize(1);
		assertThat(nodeResultB.getAgentResults().get(0).getMessage().getText()).contains("output-from-A");
	}

	@Test
	void failedNodeSetsFailedStatus() {
		Agent agentA = mockAgent("Result A");
		Agent agentB = mock(Agent.class);
		when(agentB.call(anyString(), anyMap())).thenThrow(new RuntimeException("Agent B failed"));

		GraphOrchestrator orchestrator = new GraphBuilder().addNode("A", agentA)
			.addNode("B", agentB)
			.addEdge("A", "B")
			.setEntryPoint("A")
			.build();

		MultiAgentResult result = orchestrator.invoke("task");

		assertThat(result.getStatus()).isEqualTo(Status.FAILED);
		assertThat(result.getResults().get("A").getStatus()).isEqualTo(Status.COMPLETED);
		assertThat(result.getResults().get("B").getStatus()).isEqualTo(Status.FAILED);
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
