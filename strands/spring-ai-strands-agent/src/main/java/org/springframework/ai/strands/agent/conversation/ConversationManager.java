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

import org.springframework.ai.strands.agent.AgentState;

/**
 * Strategy interface for managing conversation message history.
 *
 * <p>
 * Implementations may trim, summarize, or otherwise transform the messages held in the
 * {@link AgentState} to fit within model context-window limits.
 *
 * @author Spring AI
 * @since 2.0.0
 * @see SlidingWindowConversationManager
 * @see SummarizingConversationManager
 * @see NullConversationManager
 */
public interface ConversationManager {

	/**
	 * Apply conversation management logic to the given state. This may modify the message
	 * list in-place.
	 * @param state the agent state to manage, must not be {@code null}
	 */
	void apply(AgentState state);

}
