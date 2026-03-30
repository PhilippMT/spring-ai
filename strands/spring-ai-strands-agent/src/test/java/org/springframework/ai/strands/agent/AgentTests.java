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

package org.springframework.ai.strands.agent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.strands.agent.loop.MaxIterationsExceededException;
import org.springframework.ai.strands.hooks.HookRegistry;
import org.springframework.ai.strands.hooks.events.BeforeModelCallEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link Agent}.
 *
 * @author Spring AI
 */
@ExtendWith(MockitoExtension.class)
class AgentTests {

	@Mock
	private ChatModel chatModel;

	@Mock
	private ToolCallingManager toolCallingManager;

	@BeforeEach
	void setUp() {
	}

	@Test
	void testSimpleCall() {
		AssistantMessage assistantMessage = new AssistantMessage("Hello! How can I help?");
		Generation generation = new Generation(assistantMessage);
		ChatResponse response = new ChatResponse(List.of(generation));
		when(this.chatModel.call(any(Prompt.class))).thenReturn(response);

		Agent agent = Agent.builder().chatModel(this.chatModel).toolCallingManager(this.toolCallingManager).build();

		AgentResult result = agent.call("Hi");

		assertThat(result).isNotNull();
		assertThat(result.getMessage().getText()).isEqualTo("Hello! How can I help?");
		assertThat(result.getStopReason()).isEqualTo("end_turn");
		assertThat(result.getState()).isNotNull();
		verify(this.chatModel, times(1)).call(any(Prompt.class));
	}

	@Test
	void testCallWithToolExecution() {
		AssistantMessage toolCallMessage = AssistantMessage.builder()
			.toolCalls(List.of(new AssistantMessage.ToolCall("1", "function", "calculate", "{\"a\": 1}")))
			.build();
		Generation toolGeneration = new Generation(toolCallMessage);
		ChatResponse toolResponse = ChatResponse.builder().generations(List.of(toolGeneration)).build();

		AssistantMessage finalMessage = new AssistantMessage("The result is 42.");
		Generation finalGeneration = new Generation(finalMessage);
		ChatResponse finalResponse = new ChatResponse(List.of(finalGeneration));

		when(this.chatModel.call(any(Prompt.class))).thenReturn(toolResponse, finalResponse);

		when(this.toolCallingManager.executeToolCalls(any(Prompt.class), any(ChatResponse.class)))
			.thenReturn(ToolExecutionResult.builder()
				.conversationHistory(List.of(new org.springframework.ai.chat.messages.UserMessage("Hi"),
						toolCallMessage, new AssistantMessage("tool result")))
				.build());

		Agent agent = Agent.builder().chatModel(this.chatModel).toolCallingManager(this.toolCallingManager).build();

		AgentResult result = agent.call("Calculate something");

		assertThat(result).isNotNull();
		assertThat(result.getMessage().getText()).isEqualTo("The result is 42.");
		verify(this.chatModel, times(2)).call(any(Prompt.class));
		verify(this.toolCallingManager, times(1)).executeToolCalls(any(Prompt.class), any(ChatResponse.class));
	}

	@Test
	void testMaxIterationsExceeded() {
		AssistantMessage toolCallMessage = AssistantMessage.builder()
			.toolCalls(List.of(new AssistantMessage.ToolCall("1", "function", "loop_tool", "{}")))
			.build();
		Generation generation = new Generation(toolCallMessage);
		ChatResponse response = ChatResponse.builder().generations(List.of(generation)).build();

		when(this.chatModel.call(any(Prompt.class))).thenReturn(response);
		when(this.toolCallingManager.executeToolCalls(any(Prompt.class), any(ChatResponse.class)))
			.thenReturn(ToolExecutionResult.builder()
				.conversationHistory(
						List.of(new org.springframework.ai.chat.messages.UserMessage("Hi"), toolCallMessage))
				.build());

		Agent agent = Agent.builder()
			.chatModel(this.chatModel)
			.toolCallingManager(this.toolCallingManager)
			.maxIterations(2)
			.build();

		assertThatThrownBy(() -> agent.call("Loop forever")).isInstanceOf(MaxIterationsExceededException.class)
			.hasMessageContaining("2");
	}

	@Test
	void testHookEventsAreFired() {
		AssistantMessage assistantMessage = new AssistantMessage("Done");
		Generation generation = new Generation(assistantMessage);
		ChatResponse response = new ChatResponse(List.of(generation));
		when(this.chatModel.call(any(Prompt.class))).thenReturn(response);

		AtomicBoolean hookFired = new AtomicBoolean(false);
		HookRegistry registry = new HookRegistry();
		registry.addCallback(BeforeModelCallEvent.class, event -> hookFired.set(true));

		Agent agent = Agent.builder()
			.chatModel(this.chatModel)
			.toolCallingManager(this.toolCallingManager)
			.hookRegistry(registry)
			.build();
		agent.getHookRegistry().addCallback(BeforeModelCallEvent.class, event -> hookFired.set(true));

		agent.call("Test hooks");

		assertThat(hookFired).isTrue();
	}

	@Test
	void testWithCustomSystemPrompt() {
		AssistantMessage assistantMessage = new AssistantMessage("I am a helpful assistant.");
		Generation generation = new Generation(assistantMessage);
		ChatResponse response = new ChatResponse(List.of(generation));
		when(this.chatModel.call(any(Prompt.class))).thenReturn(response);

		Agent agent = Agent.builder()
			.chatModel(this.chatModel)
			.toolCallingManager(this.toolCallingManager)
			.systemPrompt("You are a helpful assistant.")
			.build();

		AgentResult result = agent.call("Hello");

		assertThat(result.getMessage().getText()).isEqualTo("I am a helpful assistant.");
		verify(this.chatModel).call(any(Prompt.class));
	}

	@Test
	void testCallWithInvocationState() {
		AssistantMessage assistantMessage = new AssistantMessage("OK");
		Generation generation = new Generation(assistantMessage);
		ChatResponse response = new ChatResponse(List.of(generation));
		when(this.chatModel.call(any(Prompt.class))).thenReturn(response);

		Agent agent = Agent.builder().chatModel(this.chatModel).toolCallingManager(this.toolCallingManager).build();

		AgentResult result = agent.call("Test", Map.of("key", "value"));

		assertThat(result).isNotNull();
		assertThat(result.getMessage().getText()).isEqualTo("OK");
	}

}
