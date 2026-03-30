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

package org.springframework.ai.strands.hooks.events;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for all hook event types.
 *
 * @author Spring AI
 */
class HookEventTests {

	@Nested
	class AgentInitializedEventTests {

		@Test
		void shouldCreateWithAgentId() {
			AgentInitializedEvent event = new AgentInitializedEvent("my-agent");
			assertThat(event.getAgentId()).isEqualTo("my-agent");
		}

		@Test
		void shouldNotReverseCallbacks() {
			AgentInitializedEvent event = new AgentInitializedEvent("agent");
			assertThat(event.shouldReverseCallbacks()).isFalse();
		}

		@Test
		void shouldRejectNullAgentId() {
			assertThatThrownBy(() -> new AgentInitializedEvent(null)).isInstanceOf(IllegalArgumentException.class);
		}

	}

	@Nested
	class BeforeInvocationEventTests {

		@Test
		void shouldCreateWithAllFields() {
			Map<String, Object> state = Map.of("key", "value");
			List<Message> messages = List.of(new UserMessage("hello"));
			BeforeInvocationEvent event = new BeforeInvocationEvent("agent", state, messages);

			assertThat(event.getAgentId()).isEqualTo("agent");
			assertThat(event.getInvocationState()).containsEntry("key", "value");
			assertThat(event.getMessages()).hasSize(1);
		}

		@Test
		void shouldNotReverseCallbacks() {
			BeforeInvocationEvent event = new BeforeInvocationEvent("agent", Collections.emptyMap(), List.of());
			assertThat(event.shouldReverseCallbacks()).isFalse();
		}

		@Test
		void shouldAllowSettingMessages() {
			BeforeInvocationEvent event = new BeforeInvocationEvent("agent", Collections.emptyMap(), List.of());
			List<Message> newMessages = List.of(new UserMessage("updated"));
			event.setMessages(newMessages);
			assertThat(event.getMessages()).isEqualTo(newMessages);
		}

		@Test
		void shouldHaveUnmodifiableInvocationState() {
			Map<String, Object> state = new java.util.HashMap<>();
			state.put("key", "value");
			BeforeInvocationEvent event = new BeforeInvocationEvent("agent", state, List.of());

			assertThatThrownBy(() -> event.getInvocationState().put("new", "value"))
				.isInstanceOf(UnsupportedOperationException.class);
		}

		@Test
		void shouldRejectNullMessages() {
			assertThatThrownBy(() -> new BeforeInvocationEvent("agent", Collections.emptyMap(), null))
				.isInstanceOf(IllegalArgumentException.class);
		}

	}

	@Nested
	class AfterInvocationEventTests {

		@Test
		void shouldCreateWithAllFields() {
			Map<String, Object> state = Map.of("key", "value");
			AfterInvocationEvent event = new AfterInvocationEvent("agent", state, "result");

			assertThat(event.getAgentId()).isEqualTo("agent");
			assertThat(event.getInvocationState()).containsEntry("key", "value");
			assertThat(event.getResult()).isEqualTo("result");
			assertThat(event.isResume()).isFalse();
		}

		@Test
		void shouldReverseCallbacks() {
			AfterInvocationEvent event = new AfterInvocationEvent("agent", Collections.emptyMap(), null);
			assertThat(event.shouldReverseCallbacks()).isTrue();
		}

		@Test
		void shouldAllowSettingResume() {
			AfterInvocationEvent event = new AfterInvocationEvent("agent", Collections.emptyMap(), null);
			event.setResume(true);
			assertThat(event.isResume()).isTrue();
		}

		@Test
		void shouldAcceptNullResult() {
			AfterInvocationEvent event = new AfterInvocationEvent("agent", Collections.emptyMap(), null);
			assertThat(event.getResult()).isNull();
		}

	}

	@Nested
	class MessageAddedEventTests {

		@Test
		void shouldCreateWithMessage() {
			Message message = new UserMessage("hello");
			MessageAddedEvent event = new MessageAddedEvent("agent", message);

			assertThat(event.getAgentId()).isEqualTo("agent");
			assertThat(event.getMessage()).isSameAs(message);
		}

		@Test
		void shouldNotReverseCallbacks() {
			MessageAddedEvent event = new MessageAddedEvent("agent", new UserMessage("hello"));
			assertThat(event.shouldReverseCallbacks()).isFalse();
		}

		@Test
		void shouldRejectNullMessage() {
			assertThatThrownBy(() -> new MessageAddedEvent("agent", null)).isInstanceOf(IllegalArgumentException.class);
		}

	}

	@Nested
	class BeforeToolCallEventTests {

		@Test
		void shouldCreateWithAllFields() {
			Map<String, Object> input = Map.of("param", "value");
			BeforeToolCallEvent event = new BeforeToolCallEvent("agent", "myTool", input);

			assertThat(event.getAgentId()).isEqualTo("agent");
			assertThat(event.getToolName()).isEqualTo("myTool");
			assertThat(event.getToolInput()).containsEntry("param", "value");
			assertThat(event.isCancelTool()).isFalse();
		}

		@Test
		void shouldNotReverseCallbacks() {
			BeforeToolCallEvent event = new BeforeToolCallEvent("agent", "tool", Collections.emptyMap());
			assertThat(event.shouldReverseCallbacks()).isFalse();
		}

		@Test
		void shouldAllowSettingCancelTool() {
			BeforeToolCallEvent event = new BeforeToolCallEvent("agent", "tool", Collections.emptyMap());
			event.setCancelTool(true);
			assertThat(event.isCancelTool()).isTrue();
		}

		@Test
		void shouldHaveUnmodifiableToolInput() {
			Map<String, Object> input = new java.util.HashMap<>();
			input.put("param", "value");
			BeforeToolCallEvent event = new BeforeToolCallEvent("agent", "tool", input);

			assertThatThrownBy(() -> event.getToolInput().put("new", "value"))
				.isInstanceOf(UnsupportedOperationException.class);
		}

	}

	@Nested
	class AfterToolCallEventTests {

		@Test
		void shouldCreateWithAllFields() {
			Exception ex = new RuntimeException("error");
			AfterToolCallEvent event = new AfterToolCallEvent("agent", "myTool", "result", ex);

			assertThat(event.getAgentId()).isEqualTo("agent");
			assertThat(event.getToolName()).isEqualTo("myTool");
			assertThat(event.getResult()).isEqualTo("result");
			assertThat(event.getException()).isSameAs(ex);
			assertThat(event.isRetry()).isFalse();
		}

		@Test
		void shouldReverseCallbacks() {
			AfterToolCallEvent event = new AfterToolCallEvent("agent", "tool", null, null);
			assertThat(event.shouldReverseCallbacks()).isTrue();
		}

		@Test
		void shouldAllowSettingRetry() {
			AfterToolCallEvent event = new AfterToolCallEvent("agent", "tool", null, null);
			event.setRetry(true);
			assertThat(event.isRetry()).isTrue();
		}

		@Test
		void shouldAcceptNullResultAndException() {
			AfterToolCallEvent event = new AfterToolCallEvent("agent", "tool", null, null);
			assertThat(event.getResult()).isNull();
			assertThat(event.getException()).isNull();
		}

	}

	@Nested
	class BeforeModelCallEventTests {

		@Test
		void shouldCreateWithInvocationState() {
			Map<String, Object> state = Map.of("key", "value");
			BeforeModelCallEvent event = new BeforeModelCallEvent("agent", state);

			assertThat(event.getAgentId()).isEqualTo("agent");
			assertThat(event.getInvocationState()).containsEntry("key", "value");
		}

		@Test
		void shouldNotReverseCallbacks() {
			BeforeModelCallEvent event = new BeforeModelCallEvent("agent", Collections.emptyMap());
			assertThat(event.shouldReverseCallbacks()).isFalse();
		}

	}

	@Nested
	class AfterModelCallEventTests {

		@Test
		void shouldCreateWithAllFields() {
			Map<String, Object> state = Map.of("key", "value");
			Message response = new AssistantMessage("response");
			Exception ex = new RuntimeException("error");
			AfterModelCallEvent event = new AfterModelCallEvent("agent", state, "end_turn", response, ex);

			assertThat(event.getAgentId()).isEqualTo("agent");
			assertThat(event.getInvocationState()).containsEntry("key", "value");
			assertThat(event.getStopReason()).isEqualTo("end_turn");
			assertThat(event.getResponseMessage()).isSameAs(response);
			assertThat(event.getException()).isSameAs(ex);
			assertThat(event.isRetry()).isFalse();
		}

		@Test
		void shouldReverseCallbacks() {
			AfterModelCallEvent event = new AfterModelCallEvent("agent", Collections.emptyMap(), null, null, null);
			assertThat(event.shouldReverseCallbacks()).isTrue();
		}

		@Test
		void shouldAllowSettingRetry() {
			AfterModelCallEvent event = new AfterModelCallEvent("agent", Collections.emptyMap(), null, null, null);
			event.setRetry(true);
			assertThat(event.isRetry()).isTrue();
		}

		@Test
		void shouldAcceptNullOptionalFields() {
			AfterModelCallEvent event = new AfterModelCallEvent("agent", Collections.emptyMap(), null, null, null);
			assertThat(event.getStopReason()).isNull();
			assertThat(event.getResponseMessage()).isNull();
			assertThat(event.getException()).isNull();
		}

	}

}
