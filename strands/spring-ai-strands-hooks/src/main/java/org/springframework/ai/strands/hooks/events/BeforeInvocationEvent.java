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

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.strands.hooks.HookEvent;
import org.springframework.util.Assert;

/**
 * Event fired before an agent processes a request.
 *
 * <p>
 * The {@link #getMessages()} field is writable and can be replaced by callbacks to modify
 * the messages that will be processed.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class BeforeInvocationEvent extends HookEvent {

	private final Map<String, Object> invocationState;

	private volatile List<Message> messages;

	/**
	 * Create a new {@code BeforeInvocationEvent}.
	 * @param agentId the identifier of the agent
	 * @param invocationState the invocation state, must not be {@code null}
	 * @param messages the initial messages, must not be {@code null}
	 */
	public BeforeInvocationEvent(String agentId, Map<String, Object> invocationState, List<Message> messages) {
		super(agentId);
		Assert.notNull(invocationState, "invocationState must not be null");
		Assert.notNull(messages, "messages must not be null");
		this.invocationState = Collections.unmodifiableMap(invocationState);
		this.messages = messages;
	}

	/**
	 * Return the invocation state map.
	 * @return an unmodifiable view of the invocation state
	 */
	public Map<String, Object> getInvocationState() {
		return this.invocationState;
	}

	/**
	 * Return the current messages list.
	 * @return the messages
	 */
	public List<Message> getMessages() {
		return this.messages;
	}

	/**
	 * Replace the messages list. This is a writable field that callbacks can modify.
	 * @param messages the new messages, must not be {@code null}
	 */
	public void setMessages(List<Message> messages) {
		Assert.notNull(messages, "messages must not be null");
		this.messages = messages;
	}

}
