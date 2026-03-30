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
 * Functional interface for hook callbacks that handle lifecycle events.
 *
 * <p>
 * Callbacks are registered with a {@link HookRegistry} and invoked when matching events
 * are fired during an agent's lifecycle. Each callback receives the event instance and
 * may inspect its data or modify writable fields.
 *
 * @param <T> the type of hook event this callback handles
 * @author Spring AI
 * @since 2.0.0
 */
@FunctionalInterface
public interface HookCallback<T extends BaseHookEvent> {

	/**
	 * Handle the given hook event.
	 * @param event the lifecycle event to handle
	 */
	void handle(T event);

}
