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

package org.springframework.ai.strands.agent.conversation;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.strands.agent.AgentState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SlidingWindowConversationManager}.
 *
 * @author Spring AI
 */
class SlidingWindowConversationManagerTests {

	@Test
	void testWindowKeepsLastNMessages() {
		SlidingWindowConversationManager manager = new SlidingWindowConversationManager(3);
		AgentState state = new AgentState();
		state.addMessage(new UserMessage("msg1"));
		state.addMessage(new UserMessage("msg2"));
		state.addMessage(new UserMessage("msg3"));
		state.addMessage(new UserMessage("msg4"));
		state.addMessage(new UserMessage("msg5"));

		manager.apply(state);

		List<Message> messages = state.getMessages();
		assertThat(messages).hasSize(3);
		assertThat(messages.get(0).getText()).isEqualTo("msg3");
		assertThat(messages.get(1).getText()).isEqualTo("msg4");
		assertThat(messages.get(2).getText()).isEqualTo("msg5");
	}

	@Test
	void testPreservesSystemMessage() {
		SlidingWindowConversationManager manager = new SlidingWindowConversationManager(2);
		AgentState state = new AgentState();
		state.addMessage(new SystemMessage("You are a helpful assistant."));
		state.addMessage(new UserMessage("msg1"));
		state.addMessage(new UserMessage("msg2"));
		state.addMessage(new UserMessage("msg3"));
		state.addMessage(new UserMessage("msg4"));

		manager.apply(state);

		List<Message> messages = state.getMessages();
		assertThat(messages).hasSize(3);
		assertThat(messages.get(0).getText()).isEqualTo("You are a helpful assistant.");
		assertThat(messages.get(1).getText()).isEqualTo("msg3");
		assertThat(messages.get(2).getText()).isEqualTo("msg4");
	}

	@Test
	void testNoTrimWhenBelowWindow() {
		SlidingWindowConversationManager manager = new SlidingWindowConversationManager(10);
		AgentState state = new AgentState();
		state.addMessage(new UserMessage("msg1"));
		state.addMessage(new UserMessage("msg2"));

		manager.apply(state);

		List<Message> messages = state.getMessages();
		assertThat(messages).hasSize(2);
	}

	@Test
	void testGetWindowSize() {
		SlidingWindowConversationManager manager = new SlidingWindowConversationManager(7);
		assertThat(manager.getWindowSize()).isEqualTo(7);
	}

}
