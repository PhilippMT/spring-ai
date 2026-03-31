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

package org.springframework.ai.strands.agent.output;

import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.strands.agent.AgentResult;
import org.springframework.ai.strands.agent.AgentState;
import org.springframework.ai.strands.agent.output.StructuredOutputHandler.StructuredOutputException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link StructuredOutputHandler}.
 *
 * @author Spring AI
 */
class StructuredOutputHandlerTests {

	private final StructuredOutputHandler<TestOutput> handler = new StructuredOutputHandler<>(TestOutput.class);

	@Test
	void parseValidJson() {
		AgentResult result = createResult("{\"name\":\"Alice\",\"score\":42}");

		TestOutput output = this.handler.parse(result);

		assertThat(output.name).isEqualTo("Alice");
		assertThat(output.score).isEqualTo(42);
	}

	@Test
	void parseInvalidJsonThrows() {
		AgentResult result = createResult("this is not json");

		assertThatThrownBy(() -> this.handler.parse(result)).isInstanceOf(StructuredOutputException.class)
			.hasMessageContaining("Failed to parse");
	}

	@Test
	void parseEmptyContentThrows() {
		AgentResult result = createResult("");

		assertThatThrownBy(() -> this.handler.parse(result)).isInstanceOf(StructuredOutputException.class)
			.hasMessageContaining("no text content");
	}

	@Test
	void schemaInstructionContainsTypeName() {
		String instruction = this.handler.getSchemaInstruction();

		assertThat(instruction).contains("TestOutput");
		assertThat(instruction).contains("JSON");
		assertThat(instruction).contains("name");
		assertThat(instruction).contains("score");
	}

	@Test
	void customObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		StructuredOutputHandler<TestOutput> customHandler = new StructuredOutputHandler<>(TestOutput.class, mapper);
		AgentResult result = createResult("{\"name\":\"Bob\",\"score\":99}");

		TestOutput output = customHandler.parse(result);

		assertThat(output.name).isEqualTo("Bob");
		assertThat(output.score).isEqualTo(99);
	}

	private AgentResult createResult(String text) {
		AssistantMessage message = new AssistantMessage(text);
		return new AgentResult(message, "end_turn", new AgentState(), new HashMap<>());
	}

	static class TestOutput {

		public String name;

		public int score;

	}

}
