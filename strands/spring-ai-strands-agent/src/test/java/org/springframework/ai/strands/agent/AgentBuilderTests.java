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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.strands.agent.conversation.NullConversationManager;
import org.springframework.ai.strands.agent.conversation.SlidingWindowConversationManager;
import org.springframework.ai.strands.agent.session.InMemorySessionManager;
import org.springframework.ai.strands.hooks.HookRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link AgentBuilder}.
 *
 * @author Spring AI
 */
@ExtendWith(MockitoExtension.class)
class AgentBuilderTests {

	@Mock
	private ChatModel chatModel;

	@Mock
	private ToolCallingManager toolCallingManager;

	@Test
	void testChatModelIsRequired() {
		assertThatIllegalStateException().isThrownBy(() -> Agent.builder().build()).withMessageContaining("chatModel");
	}

	@Test
	void testDefaultValues() {
		Agent agent = Agent.builder().chatModel(this.chatModel).toolCallingManager(this.toolCallingManager).build();

		assertThat(agent).isNotNull();
		assertThat(agent.getAgentId()).isNotNull().isNotEmpty();
	}

	@Test
	void testAllBuilderOptions() {
		Agent agent = Agent.builder()
			.chatModel(this.chatModel)
			.toolCallingManager(this.toolCallingManager)
			.systemPrompt("You are a test agent.")
			.maxIterations(10)
			.conversationManager(new SlidingWindowConversationManager(5))
			.sessionManager(new InMemorySessionManager())
			.hookRegistry(new HookRegistry())
			.build();

		assertThat(agent).isNotNull();
		assertThat(agent.getAgentId()).isNotNull();
	}

	@Test
	void testBuilderWithNullConversationManager() {
		Agent agent = Agent.builder()
			.chatModel(this.chatModel)
			.toolCallingManager(this.toolCallingManager)
			.conversationManager(new NullConversationManager())
			.build();

		assertThat(agent).isNotNull();
	}

	@Test
	void testBuilderMaxIterationsMustBePositive() {
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> Agent.builder().maxIterations(0));
	}

}
