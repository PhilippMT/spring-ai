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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.strands.agent.AgentResult;
import org.springframework.util.Assert;

/**
 * Provides structured output support for agent responses, porting the Strands
 * {@code StructuredOutputTool} concept. Extracts typed objects from an
 * {@link AgentResult} by parsing the assistant message content as JSON.
 *
 * @param <T> the target output type
 * @author Spring AI
 * @since 2.0.0
 */
public class StructuredOutputHandler<T> {

	private final Class<T> outputType;

	private final ObjectMapper objectMapper;

	/**
	 * Create a new handler for the given output type using a default
	 * {@link ObjectMapper}.
	 * @param outputType the class of the target type, must not be {@code null}
	 */
	public StructuredOutputHandler(Class<T> outputType) {
		this(outputType, new ObjectMapper());
	}

	/**
	 * Create a new handler for the given output type and {@link ObjectMapper}.
	 * @param outputType the class of the target type, must not be {@code null}
	 * @param objectMapper the Jackson object mapper to use, must not be {@code null}
	 */
	public StructuredOutputHandler(Class<T> outputType, ObjectMapper objectMapper) {
		Assert.notNull(outputType, "outputType must not be null");
		Assert.notNull(objectMapper, "objectMapper must not be null");
		this.outputType = outputType;
		this.objectMapper = objectMapper;
	}

	/**
	 * Extract structured output from the agent response by parsing the assistant message
	 * content as JSON and mapping it to the target type.
	 * @param result the agent result to parse, must not be {@code null}
	 * @return the parsed object of the target type
	 * @throws StructuredOutputException if the response cannot be parsed
	 */
	public T parse(AgentResult result) {
		Assert.notNull(result, "result must not be null");
		String text = result.getMessage().getText();
		if (text == null || text.isBlank()) {
			throw new StructuredOutputException("Agent response contains no text content");
		}
		try {
			return this.objectMapper.readValue(text, this.outputType);
		}
		catch (JsonProcessingException ex) {
			throw new StructuredOutputException("Failed to parse agent response as " + this.outputType.getSimpleName(),
					ex);
		}
	}

	/**
	 * Create a system prompt suffix that instructs the model to produce JSON output
	 * matching the target schema.
	 * @return the schema instruction string
	 */
	public String getSchemaInstruction() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n\nYou must respond with valid JSON that conforms to the following structure:\n");
		sb.append("Type: ").append(this.outputType.getSimpleName()).append("\n");
		sb.append("Fields:\n");
		for (java.lang.reflect.Field field : this.outputType.getDeclaredFields()) {
			if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				sb.append("  - ").append(field.getName()).append(": ").append(field.getType().getSimpleName());
				sb.append("\n");
			}
		}
		sb.append("\nRespond ONLY with the JSON object, no additional text.");
		return sb.toString();
	}

	/**
	 * Exception thrown when structured output parsing fails.
	 */
	public static class StructuredOutputException extends RuntimeException {

		public StructuredOutputException(String message) {
			super(message);
		}

		public StructuredOutputException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
