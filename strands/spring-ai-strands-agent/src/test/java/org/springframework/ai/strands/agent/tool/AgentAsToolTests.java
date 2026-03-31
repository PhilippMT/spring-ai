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

package org.springframework.ai.strands.agent.tool;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.strands.agent.Agent;
import org.springframework.ai.strands.agent.AgentResult;
import org.springframework.ai.strands.agent.AgentState;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AgentAsTool}.
 *
 * @author Spring AI
 */
@ExtendWith(MockitoExtension.class)
class AgentAsToolTests {

	@Mock
	private Agent agent;

	@Test
	void toolDefinitionHasCorrectNameAndDescription() {
		AgentAsTool tool = new AgentAsTool(this.agent, "research-agent", "Performs research tasks");

		ToolDefinition definition = tool.getToolDefinition();

		assertThat(definition.name()).isEqualTo("research-agent");
		assertThat(definition.description()).isEqualTo("Performs research tasks");
		assertThat(definition.inputSchema()).contains("input");
	}

	@Test
	void callDelegatesToAgent() {
		AgentResult result = createResult("The answer is 42");
		when(this.agent.call(anyString())).thenReturn(result);

		AgentAsTool tool = new AgentAsTool(this.agent, "qa-agent", "Answers questions");
		String response = tool.call("{\"input\":\"What is the answer?\"}");

		assertThat(response).isEqualTo("The answer is 42");
		verify(this.agent).call("What is the answer?");
	}

	@Test
	void callWithPlainStringInput() {
		AgentResult result = createResult("Response text");
		when(this.agent.call(anyString())).thenReturn(result);

		AgentAsTool tool = new AgentAsTool(this.agent, "agent", "An agent");
		String response = tool.call("plain text input");

		assertThat(response).isEqualTo("Response text");
	}

	@Test
	void callWrapsAgentExceptionInToolExecutionException() {
		when(this.agent.call(anyString())).thenThrow(new RuntimeException("Agent failed"));

		AgentAsTool tool = new AgentAsTool(this.agent, "failing-agent", "An agent that fails");

		assertThatThrownBy(() -> tool.call("{\"input\":\"test\"}")).isInstanceOf(ToolExecutionException.class)
			.hasCauseInstanceOf(RuntimeException.class);
	}

	@Test
	void callHandlesNullResponseText() {
		AgentResult result = createResult(null);
		when(this.agent.call(anyString())).thenReturn(result);

		AgentAsTool tool = new AgentAsTool(this.agent, "agent", "An agent");
		String response = tool.call("{\"input\":\"test\"}");

		assertThat(response).isEmpty();
	}

	private AgentResult createResult(String text) {
		AssistantMessage message = new AssistantMessage(text);
		return new AgentResult(message, "end_turn", new AgentState(), new HashMap<>());
	}

}
