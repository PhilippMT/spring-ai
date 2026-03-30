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

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.strands.agent.AgentState;
import org.springframework.util.Assert;

/**
 * A {@link ConversationManager} that summarizes older messages using the chat model when
 * the conversation exceeds a configured threshold.
 *
 * <p>
 * System messages are always preserved. When the number of non-system messages exceeds
 * the threshold, the older messages are summarized into a single system message and
 * replaced with it, retaining only the most recent messages alongside the summary.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class SummarizingConversationManager implements ConversationManager {

	private static final Logger logger = LoggerFactory.getLogger(SummarizingConversationManager.class);

	private static final String SUMMARY_PROMPT = "Summarize the following conversation concisely, "
			+ "preserving key facts and context:\n\n";

	private final ChatModel chatModel;

	private final int threshold;

	private final int keepRecent;

	/**
	 * Create a new {@code SummarizingConversationManager}.
	 * @param chatModel the model used to generate summaries, must not be {@code null}
	 * @param threshold the number of non-system messages that triggers summarization,
	 * must be positive
	 * @param keepRecent the number of recent non-system messages to keep after
	 * summarization, must be positive and less than the threshold
	 */
	public SummarizingConversationManager(ChatModel chatModel, int threshold, int keepRecent) {
		Assert.notNull(chatModel, "chatModel must not be null");
		Assert.isTrue(threshold > 0, "threshold must be positive");
		Assert.isTrue(keepRecent > 0, "keepRecent must be positive");
		Assert.isTrue(keepRecent < threshold, "keepRecent must be less than threshold");
		this.chatModel = chatModel;
		this.threshold = threshold;
		this.keepRecent = keepRecent;
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

		if (nonSystemMessages.size() <= this.threshold) {
			return;
		}

		int splitPoint = nonSystemMessages.size() - this.keepRecent;
		List<Message> toSummarize = nonSystemMessages.subList(0, splitPoint);
		List<Message> toKeep = nonSystemMessages.subList(splitPoint, nonSystemMessages.size());

		StringBuilder conversationText = new StringBuilder();
		for (Message msg : toSummarize) {
			conversationText.append(msg.getMessageType().getValue())
				.append(": ")
				.append(msg.getText() != null ? msg.getText() : "")
				.append("\n");
		}

		@Nullable String summary = summarize(conversationText.toString());

		List<Message> result = new ArrayList<>(systemMessages);
		if (summary != null) {
			result.add(new SystemMessage("Previous conversation summary: " + summary));
		}
		result.addAll(toKeep);
		state.setMessages(result);
	}

	private @Nullable String summarize(String conversationText) {
		try {
			ChatResponse response = this.chatModel.call(new Prompt(SUMMARY_PROMPT + conversationText));
			if (response.getResult() != null) {
				AssistantMessage output = response.getResult().getOutput();
				return output.getText();
			}
		}
		catch (Exception ex) {
			logger.warn("Failed to generate conversation summary, keeping messages as-is", ex);
		}
		return null;
	}

}
