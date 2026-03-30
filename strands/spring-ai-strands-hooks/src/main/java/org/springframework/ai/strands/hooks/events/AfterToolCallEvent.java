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

import org.springframework.ai.strands.hooks.HookEvent;
import org.springframework.util.Assert;

/**
 * Event fired after a tool has been executed.
 *
 * <p>
 * Callbacks are invoked in reverse registration order (LIFO) for this event type. The
 * {@link #isRetry()} field is writable and can be set by callbacks to indicate that the
 * tool execution should be retried.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class AfterToolCallEvent extends HookEvent {

	private final String toolName;

	private final Object result;

	private final Exception exception;

	private volatile boolean retry;

	/**
	 * Create a new {@code AfterToolCallEvent}.
	 * @param agentId the identifier of the agent
	 * @param toolName the name of the tool that was executed, must not be {@code null}
	 * @param result the result of the tool execution, may be {@code null}
	 * @param exception the exception that occurred during execution, or {@code null} if
	 * successful
	 */
	public AfterToolCallEvent(String agentId, String toolName, Object result, Exception exception) {
		super(agentId);
		Assert.notNull(toolName, "toolName must not be null");
		this.toolName = toolName;
		this.result = result;
		this.exception = exception;
		this.retry = false;
	}

	@Override
	public boolean shouldReverseCallbacks() {
		return true;
	}

	/**
	 * Return the name of the tool that was executed.
	 * @return the tool name, never {@code null}
	 */
	public String getToolName() {
		return this.toolName;
	}

	/**
	 * Return the result of the tool execution.
	 * @return the result, may be {@code null}
	 */
	public Object getResult() {
		return this.result;
	}

	/**
	 * Return the exception that occurred during tool execution, if any.
	 * @return the exception, or {@code null} if the tool executed successfully
	 */
	public Exception getException() {
		return this.exception;
	}

	/**
	 * Return whether the tool execution should be retried.
	 * @return {@code true} if the tool should be retried, {@code false} otherwise
	 */
	public boolean isRetry() {
		return this.retry;
	}

	/**
	 * Set whether the tool execution should be retried. This is a writable field that
	 * callbacks can modify.
	 * @param retry {@code true} to retry the tool execution
	 */
	public void setRetry(boolean retry) {
		this.retry = retry;
	}

}
