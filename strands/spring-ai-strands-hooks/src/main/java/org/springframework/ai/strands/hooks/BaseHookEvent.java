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

package org.springframework.ai.strands.hooks;

/**
 * Base class for all hook events in the lifecycle hook system.
 *
 * <p>
 * Hook events represent specific points in an agent's lifecycle where callbacks can be
 * invoked. Subclasses define the data available at each lifecycle point.
 *
 * <p>
 * Events that represent completion or cleanup phases (e.g., after-invocation,
 * after-tool-call) should override {@link #shouldReverseCallbacks()} to return
 * {@code true}, ensuring callbacks are invoked in reverse registration order (LIFO).
 *
 * @author Spring AI
 * @since 2.0.0
 */
public abstract class BaseHookEvent {

	/**
	 * Returns whether callbacks for this event type should be invoked in reverse
	 * registration order.
	 *
	 * <p>
	 * Override this method and return {@code true} for cleanup or completion events to
	 * ensure that callbacks registered last are invoked first (LIFO ordering), mirroring
	 * the Strands Agents SDK behavior.
	 * @return {@code true} if callbacks should be invoked in reverse order, {@code false}
	 * otherwise
	 */
	public boolean shouldReverseCallbacks() {
		return false;
	}

}
