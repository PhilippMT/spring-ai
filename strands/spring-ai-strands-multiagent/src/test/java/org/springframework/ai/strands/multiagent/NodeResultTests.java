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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.strands.agent.AgentResult;
import org.springframework.ai.strands.agent.AgentState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NodeResult}.
 *
 * @author Spring AI
 */
class NodeResultTests {

	@Test
	void nodeResultCreation() {
		AgentResult agentResult = createAgentResult("test output");
		NodeResult nodeResult = NodeResult.fromAgentResult(agentResult, 100L);

		assertThat(nodeResult.getStatus()).isEqualTo(Status.COMPLETED);
		assertThat(nodeResult.getExecutionTimeMs()).isEqualTo(100L);
		assertThat(nodeResult.getExecutionCount()).isEqualTo(1);
		assertThat(nodeResult.getResult()).isInstanceOf(NodeResult.AgentResultValue.class);
	}

	@Test
	void getAgentResultsFlattening() {
		AgentResult result1 = createAgentResult("result 1");
		AgentResult result2 = createAgentResult("result 2");

		NodeResult nodeResult1 = NodeResult.fromAgentResult(result1, 50L);
		NodeResult nodeResult2 = NodeResult.fromAgentResult(result2, 60L);

		Map<String, NodeResult> innerResults = new LinkedHashMap<>();
		innerResults.put("node1", nodeResult1);
		innerResults.put("node2", nodeResult2);
		MultiAgentResult multiAgentResult = new MultiAgentResult(Status.COMPLETED, innerResults, 0, 0, 0, 110, 2, 110);

		NodeResult nestedNodeResult = new NodeResult(new NodeResult.MultiAgentResultValue(multiAgentResult), 110L,
				Status.COMPLETED, 0, 0, 0, 110L, 2);

		List<AgentResult> flattened = nestedNodeResult.getAgentResults();
		assertThat(flattened).hasSize(2);
		assertThat(flattened.get(0).getMessage().getText()).isEqualTo("result 1");
		assertThat(flattened.get(1).getMessage().getText()).isEqualTo("result 2");
	}

	@Test
	void exceptionResultStatus() {
		NodeResult nodeResult = NodeResult.fromException(new RuntimeException("test error"), 25L);

		assertThat(nodeResult.getStatus()).isEqualTo(Status.FAILED);
		assertThat(nodeResult.getExecutionTimeMs()).isEqualTo(25L);
		assertThat(nodeResult.getResult()).isInstanceOf(NodeResult.ExceptionResultValue.class);
		assertThat(nodeResult.getAgentResults()).isEmpty();

		NodeResult.ExceptionResultValue exValue = (NodeResult.ExceptionResultValue) nodeResult.getResult();
		assertThat(exValue.exception()).hasMessage("test error");
	}

	@Test
	void fullConstructorFields() {
		AgentResult agentResult = createAgentResult("detailed");
		NodeResult nodeResult = new NodeResult(new NodeResult.AgentResultValue(agentResult), 200L, Status.COMPLETED, 10,
				20, 30, 150L, 3);

		assertThat(nodeResult.getInputTokens()).isEqualTo(10);
		assertThat(nodeResult.getOutputTokens()).isEqualTo(20);
		assertThat(nodeResult.getTotalTokens()).isEqualTo(30);
		assertThat(nodeResult.getLatencyMs()).isEqualTo(150L);
		assertThat(nodeResult.getExecutionCount()).isEqualTo(3);
	}

	private AgentResult createAgentResult(String text) {
		AssistantMessage message = new AssistantMessage(text);
		AgentState state = new AgentState();
		Map<String, Object> metrics = new HashMap<>();
		return new AgentResult(message, "end_turn", state, metrics);
	}

}
