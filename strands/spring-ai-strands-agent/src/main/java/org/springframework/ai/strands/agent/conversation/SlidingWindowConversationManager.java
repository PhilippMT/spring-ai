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

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.strands.agent.AgentState;
import org.springframework.util.Assert;

/**
 * A {@link ConversationManager} that keeps the most recent messages within a sliding
 * window.
 *
 * <p>
 * System messages are always preserved regardless of the window size. If the total number
 * of non-system messages exceeds the window size, the oldest non-system messages are
 * dropped.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class SlidingWindowConversationManager implements ConversationManager {

	private final int windowSize;

	/**
	 * Create a new {@code SlidingWindowConversationManager}.
	 * @param windowSize the maximum number of non-system messages to keep, must be
	 * positive
	 */
	public SlidingWindowConversationManager(int windowSize) {
		Assert.isTrue(windowSize > 0, "windowSize must be positive");
		this.windowSize = windowSize;
	}

	@Override
	public void apply(AgentState state) {
		Assert.notNull(state, "state must not be null");
		List<Message> messages = state.getMessages();
		List<Message> systemMessages = new ArrayList<>();
		List<Message> nonSystemMessages = new ArrayList<>();

		for (Message message : messages) {
			if (message.getMessageType() == MessageType.SYSTEM) {
				systemMessages.add(message);
			}
			else {
				nonSystemMessages.add(message);
			}
		}

		if (nonSystemMessages.size() <= this.windowSize) {
			return;
		}

		List<Message> trimmed = new ArrayList<>(systemMessages);
		int start = nonSystemMessages.size() - this.windowSize;
		trimmed.addAll(nonSystemMessages.subList(start, nonSystemMessages.size()));
		state.setMessages(trimmed);
	}

	/**
	 * Return the configured window size.
	 * @return the window size
	 */
	public int getWindowSize() {
		return this.windowSize;
	}

}
