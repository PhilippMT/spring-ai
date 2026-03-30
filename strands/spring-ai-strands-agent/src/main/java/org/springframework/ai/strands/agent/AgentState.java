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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

/**
 * Mutable state of an agent during execution.
 *
 * <p>
 * Holds the conversation messages, arbitrary attributes, and the session identifier. All
 * mutating operations are synchronized for thread safety.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class AgentState {

	private final List<Message> messages;

	private final Map<String, Object> attributes;

	private String sessionId;

	/**
	 * Create a new {@code AgentState} with an auto-generated session identifier.
	 */
	public AgentState() {
		this.messages = new ArrayList<>();
		this.attributes = new LinkedHashMap<>();
		this.sessionId = UUID.randomUUID().toString();
	}

	/**
	 * Create a new {@code AgentState} with the given session identifier.
	 * @param sessionId the session identifier, must not be {@code null}
	 */
	public AgentState(String sessionId) {
		Assert.notNull(sessionId, "sessionId must not be null");
		this.messages = new ArrayList<>();
		this.attributes = new LinkedHashMap<>();
		this.sessionId = sessionId;
	}

	/**
	 * Return a snapshot of the current messages.
	 * @return an unmodifiable list of messages
	 */
	public synchronized List<Message> getMessages() {
		return Collections.unmodifiableList(new ArrayList<>(this.messages));
	}

	/**
	 * Replace the entire message list.
	 * @param messages the new messages, must not be {@code null}
	 */
	public synchronized void setMessages(List<Message> messages) {
		Assert.notNull(messages, "messages must not be null");
		this.messages.clear();
		this.messages.addAll(messages);
	}

	/**
	 * Append a message to the conversation.
	 * @param message the message to add, must not be {@code null}
	 */
	public synchronized void addMessage(Message message) {
		Assert.notNull(message, "message must not be null");
		this.messages.add(message);
	}

	/**
	 * Return a snapshot of the current attributes.
	 * @return an unmodifiable map of attributes
	 */
	public synchronized Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(new LinkedHashMap<>(this.attributes));
	}

	/**
	 * Store an attribute.
	 * @param key the attribute key, must not be {@code null}
	 * @param value the attribute value
	 */
	public synchronized void setAttribute(String key, @Nullable Object value) {
		Assert.notNull(key, "key must not be null");
		this.attributes.put(key, value);
	}

	/**
	 * Retrieve an attribute.
	 * @param key the attribute key, must not be {@code null}
	 * @return the attribute value, or {@code null} if not present
	 */
	public synchronized @Nullable Object getAttribute(String key) {
		Assert.notNull(key, "key must not be null");
		return this.attributes.get(key);
	}

	/**
	 * Return the session identifier.
	 * @return the session identifier
	 */
	public synchronized String getSessionId() {
		return this.sessionId;
	}

	/**
	 * Set the session identifier.
	 * @param sessionId the session identifier, must not be {@code null}
	 */
	public synchronized void setSessionId(String sessionId) {
		Assert.notNull(sessionId, "sessionId must not be null");
		this.sessionId = sessionId;
	}

}
