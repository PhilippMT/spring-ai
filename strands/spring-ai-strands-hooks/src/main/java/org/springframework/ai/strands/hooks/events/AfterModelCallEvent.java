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

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.strands.hooks.HookEvent;
import org.springframework.util.Assert;

/**
 * Event fired after a model call has completed.
 *
 * <p>
 * Callbacks are invoked in reverse registration order (LIFO) for this event type. The
 * {@link #isRetry()} field is writable and can be set by callbacks to indicate that the
 * model call should be retried.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class AfterModelCallEvent extends HookEvent {

	private final Map<String, Object> invocationState;

	private final String stopReason;

	private final Message responseMessage;

	private final Exception exception;

	private volatile boolean retry;

	/**
	 * Create a new {@code AfterModelCallEvent}.
	 * @param agentId the identifier of the agent
	 * @param invocationState the invocation state, must not be {@code null}
	 * @param stopReason the reason the model stopped generating, may be {@code null}
	 * @param responseMessage the response message from the model, may be {@code null}
	 * @param exception the exception that occurred during the model call, or {@code null}
	 * if successful
	 */
	public AfterModelCallEvent(String agentId, Map<String, Object> invocationState, String stopReason,
			Message responseMessage, Exception exception) {
		super(agentId);
		Assert.notNull(invocationState, "invocationState must not be null");
		this.invocationState = Collections.unmodifiableMap(invocationState);
		this.stopReason = stopReason;
		this.responseMessage = responseMessage;
		this.exception = exception;
		this.retry = false;
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
	 * Return the reason the model stopped generating.
	 * @return the stop reason, may be {@code null}
	 */
	public String getStopReason() {
		return this.stopReason;
	}

	/**
	 * Return the response message from the model.
	 * @return the response message, may be {@code null}
	 */
	public Message getResponseMessage() {
		return this.responseMessage;
	}

	/**
	 * Return the exception that occurred during the model call, if any.
	 * @return the exception, or {@code null} if the call completed successfully
	 */
	public Exception getException() {
		return this.exception;
	}

	/**
	 * Return whether the model call should be retried.
	 * @return {@code true} if the call should be retried, {@code false} otherwise
	 */
	public boolean isRetry() {
		return this.retry;
	}

	/**
	 * Set whether the model call should be retried. This is a writable field that
	 * callbacks can modify.
	 * @param retry {@code true} to retry the model call
	 */
	public void setRetry(boolean retry) {
		this.retry = retry;
	}

}
