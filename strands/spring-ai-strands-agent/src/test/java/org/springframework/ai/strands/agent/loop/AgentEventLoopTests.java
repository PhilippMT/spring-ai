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

package org.springframework.ai.strands.agent.loop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.strands.agent.AgentOptions;
import org.springframework.ai.strands.agent.AgentState;
import org.springframework.ai.strands.hooks.HookRegistry;
import org.springframework.ai.strands.hooks.events.AfterModelCallEvent;
import org.springframework.ai.strands.hooks.events.AfterToolCallEvent;
import org.springframework.ai.strands.hooks.events.BeforeModelCallEvent;
import org.springframework.ai.strands.hooks.events.BeforeToolCallEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AgentEventLoop}.
 *
 * @author Spring AI
 */
@ExtendWith(MockitoExtension.class)
class AgentEventLoopTests {

	@Mock
	private ChatModel chatModel;

	@Mock
	private ToolCallingManager toolCallingManager;

	private AgentEventLoop eventLoop;

	private HookRegistry hookRegistry;

	private AgentState state;

	@BeforeEach
	void setUp() {
		this.eventLoop = new AgentEventLoop();
		this.hookRegistry = new HookRegistry();
		this.state = new AgentState();
		this.state.addMessage(new UserMessage("Hello"));
	}

	@Test
	void testSingleIterationNoTools() {
		AssistantMessage message = new AssistantMessage("Hi there!");
		ChatResponse response = new ChatResponse(List.of(new Generation(message)));
		when(this.chatModel.call(any(Prompt.class))).thenReturn(response);

		AgentOptions options = AgentOptions.builder().maxIterations(10).build();

		EventLoopResult result = this.eventLoop.execute(this.chatModel, this.state, options, List.of(),
				this.toolCallingManager, this.hookRegistry, "test-agent", new HashMap<>());

		assertThat(result.getMessage().getText()).isEqualTo("Hi there!");
		assertThat(result.getStopReason()).isEqualTo("end_turn");
		assertThat(result.isRequiresToolExecution()).isFalse();
		verify(this.chatModel, times(1)).call(any(Prompt.class));
	}

	@Test
	void testMultiIterationWithTools() {
		AssistantMessage toolCallMessage = AssistantMessage.builder()
			.toolCalls(List.of(new AssistantMessage.ToolCall("1", "function", "search", "{\"q\": \"test\"}")))
			.build();
		ChatResponse toolResponse = ChatResponse.builder()
			.generations(List.of(new Generation(toolCallMessage)))
			.build();

		AssistantMessage finalMessage = new AssistantMessage("Found the answer.");
		ChatResponse finalResponse = new ChatResponse(List.of(new Generation(finalMessage)));

		when(this.chatModel.call(any(Prompt.class))).thenReturn(toolResponse, finalResponse);
		when(this.toolCallingManager.executeToolCalls(any(Prompt.class), any(ChatResponse.class)))
			.thenReturn(ToolExecutionResult.builder()
				.conversationHistory(List.of(new UserMessage("Hello"), toolCallMessage))
				.build());

		AgentOptions options = AgentOptions.builder().maxIterations(10).build();

		EventLoopResult result = this.eventLoop.execute(this.chatModel, this.state, options, List.of(),
				this.toolCallingManager, this.hookRegistry, "test-agent", new HashMap<>());

		assertThat(result.getMessage().getText()).isEqualTo("Found the answer.");
		assertThat(result.getToolResults()).hasSize(1);
		verify(this.chatModel, times(2)).call(any(Prompt.class));
	}

	@Test
	void testMaxIterations() {
		AssistantMessage toolCallMessage = AssistantMessage.builder()
			.toolCalls(List.of(new AssistantMessage.ToolCall("1", "function", "loop", "{}")))
			.build();
		ChatResponse toolResponse = ChatResponse.builder()
			.generations(List.of(new Generation(toolCallMessage)))
			.build();

		when(this.chatModel.call(any(Prompt.class))).thenReturn(toolResponse);
		when(this.toolCallingManager.executeToolCalls(any(Prompt.class), any(ChatResponse.class)))
			.thenReturn(ToolExecutionResult.builder()
				.conversationHistory(List.of(new UserMessage("Hello"), toolCallMessage))
				.build());

		AgentOptions options = AgentOptions.builder().maxIterations(3).build();

		assertThatThrownBy(() -> this.eventLoop.execute(this.chatModel, this.state, options, List.of(),
				this.toolCallingManager, this.hookRegistry, "test-agent", new HashMap<>()))
			.isInstanceOf(MaxIterationsExceededException.class)
			.satisfies(ex -> assertThat(((MaxIterationsExceededException) ex).getMaxIterations()).isEqualTo(3));
	}

	@Test
	void testHookEventsFiringOrder() {
		AssistantMessage message = new AssistantMessage("Response");
		ChatResponse response = new ChatResponse(List.of(new Generation(message)));
		when(this.chatModel.call(any(Prompt.class))).thenReturn(response);

		List<String> eventOrder = new ArrayList<>();
		this.hookRegistry.addCallback(BeforeModelCallEvent.class, event -> eventOrder.add("before_model"));
		this.hookRegistry.addCallback(AfterModelCallEvent.class, event -> eventOrder.add("after_model"));

		AgentOptions options = AgentOptions.builder().maxIterations(10).build();

		this.eventLoop.execute(this.chatModel, this.state, options, List.of(), this.toolCallingManager,
				this.hookRegistry, "test-agent", new HashMap<>());

		assertThat(eventOrder).containsExactly("before_model", "after_model");
	}

	@Test
	void testToolCancelViaHook() {
		AssistantMessage toolCallMessage = AssistantMessage.builder()
			.toolCalls(List.of(new AssistantMessage.ToolCall("1", "function", "dangerous", "{}")))
			.build();
		ChatResponse toolResponse = ChatResponse.builder()
			.generations(List.of(new Generation(toolCallMessage)))
			.build();

		when(this.chatModel.call(any(Prompt.class))).thenReturn(toolResponse);

		this.hookRegistry.addCallback(BeforeToolCallEvent.class, event -> event.setCancelTool(true));

		AgentOptions options = AgentOptions.builder().maxIterations(10).build();

		EventLoopResult result = this.eventLoop.execute(this.chatModel, this.state, options, List.of(),
				this.toolCallingManager, this.hookRegistry, "test-agent", new HashMap<>());

		assertThat(result).isNotNull();
		assertThat(result.isRequiresToolExecution()).isFalse();
	}

	@Test
	void testModelRetryViaHook() {
		AssistantMessage message = new AssistantMessage("Success after retry");
		ChatResponse response = new ChatResponse(List.of(new Generation(message)));

		AtomicInteger callCount = new AtomicInteger(0);
		when(this.chatModel.call(any(Prompt.class))).thenReturn(response);

		AtomicBoolean retried = new AtomicBoolean(false);
		this.hookRegistry.addCallback(AfterModelCallEvent.class, event -> {
			if (!retried.get()) {
				retried.set(true);
				event.setRetry(true);
			}
		});

		AgentOptions options = AgentOptions.builder().maxIterations(10).build();

		EventLoopResult result = this.eventLoop.execute(this.chatModel, this.state, options, List.of(),
				this.toolCallingManager, this.hookRegistry, "test-agent", new HashMap<>());

		assertThat(result.getMessage().getText()).isEqualTo("Success after retry");
		verify(this.chatModel, times(2)).call(any(Prompt.class));
	}

	@Test
	void testToolCallEventsAreFired() {
		AssistantMessage toolCallMessage = AssistantMessage.builder()
			.toolCalls(List.of(new AssistantMessage.ToolCall("1", "function", "mytool", "{\"x\": 1}")))
			.build();
		ChatResponse toolResponse = ChatResponse.builder()
			.generations(List.of(new Generation(toolCallMessage)))
			.build();

		AssistantMessage finalMessage = new AssistantMessage("Done");
		ChatResponse finalResponse = new ChatResponse(List.of(new Generation(finalMessage)));

		when(this.chatModel.call(any(Prompt.class))).thenReturn(toolResponse, finalResponse);
		when(this.toolCallingManager.executeToolCalls(any(Prompt.class), any(ChatResponse.class)))
			.thenReturn(ToolExecutionResult.builder()
				.conversationHistory(List.of(new UserMessage("Hello"), toolCallMessage))
				.build());

		AtomicBoolean beforeToolFired = new AtomicBoolean(false);
		AtomicBoolean afterToolFired = new AtomicBoolean(false);
		this.hookRegistry.addCallback(BeforeToolCallEvent.class, event -> {
			assertThat(event.getToolName()).isEqualTo("mytool");
			beforeToolFired.set(true);
		});
		this.hookRegistry.addCallback(AfterToolCallEvent.class, event -> {
			assertThat(event.getToolName()).isEqualTo("mytool");
			afterToolFired.set(true);
		});

		AgentOptions options = AgentOptions.builder().maxIterations(10).build();

		this.eventLoop.execute(this.chatModel, this.state, options, List.of(), this.toolCallingManager,
				this.hookRegistry, "test-agent", new HashMap<>());

		assertThat(beforeToolFired).isTrue();
		assertThat(afterToolFired).isTrue();
	}

}
