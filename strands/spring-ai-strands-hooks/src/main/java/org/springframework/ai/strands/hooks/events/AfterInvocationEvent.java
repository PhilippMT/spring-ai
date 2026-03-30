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
 * Event fired after an agent completes processing a request.
 *
 * <p>
 * Callbacks are invoked in reverse registration order (LIFO) for this event type. The
 * {@link #isResume()} field is writable and can be set by callbacks to indicate that the
 * agent should resume processing.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class AfterInvocationEvent extends HookEvent {

	private final Map<String, Object> invocationState;

	private final Object result;

	private volatile boolean resume;

	/**
	 * Create a new {@code AfterInvocationEvent}.
	 * @param agentId the identifier of the agent
	 * @param invocationState the invocation state, must not be {@code null}
	 * @param result the result of the invocation, may be {@code null}
	 */
	public AfterInvocationEvent(String agentId, Map<String, Object> invocationState, Object result) {
		super(agentId);
		Assert.notNull(invocationState, "invocationState must not be null");
		this.invocationState = Collections.unmodifiableMap(invocationState);
		this.result = result;
		this.resume = false;
	}

	@Override
	public boolean shouldReverseCallbacks() {
		return true;
	}

	/**
	 * Return the invocation state map.
	 * @return an unmodifiable view of the invocation state
	 */
	public Map<String, Object> getInvocationState() {
		return this.invocationState;
	}

	/**
	 * Return the result of the invocation.
	 * @return the result, may be {@code null}
	 */
	public Object getResult() {
		return this.result;
	}

	/**
	 * Return whether the agent should resume processing.
	 * @return {@code true} if the agent should resume, {@code false} otherwise
	 */
	public boolean isResume() {
		return this.resume;
	}

	/**
	 * Set whether the agent should resume processing. This is a writable field that
	 * callbacks can modify.
	 * @param resume {@code true} to indicate the agent should resume
	 */
	public void setResume(boolean resume) {
		this.resume = resume;
	}

}
