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
import java.util.Map;

import org.springframework.ai.strands.hooks.HookEvent;
import org.springframework.util.Assert;

/**
 * Event fired before a model call is made.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class BeforeModelCallEvent extends HookEvent {

	private final Map<String, Object> invocationState;

	/**
	 * Create a new {@code BeforeModelCallEvent}.
	 * @param agentId the identifier of the agent
	 * @param invocationState the invocation state, must not be {@code null}
	 */
	public BeforeModelCallEvent(String agentId, Map<String, Object> invocationState) {
		super(agentId);
		Assert.notNull(invocationState, "invocationState must not be null");
		this.invocationState = Collections.unmodifiableMap(invocationState);
	}

	/**
	 * Return the invocation state map.
	 * @return an unmodifiable view of the invocation state
	 */
	public Map<String, Object> getInvocationState() {
		return this.invocationState;
	}

}
