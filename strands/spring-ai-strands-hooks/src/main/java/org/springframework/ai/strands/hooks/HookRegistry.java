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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.Assert;

/**
 * Thread-safe registry that manages lifecycle hook callbacks for agent events.
 *
 * <p>
 * Callbacks are registered for specific event types and invoked when matching events are
 * fired. The registry supports both normal (FIFO) and reverse (LIFO) callback ordering,
 * controlled by the event's {@link BaseHookEvent#shouldReverseCallbacks()} method.
 *
 * <p>
 * This class is thread-safe. Callback lists use {@link CopyOnWriteArrayList} to allow
 * concurrent reads and writes without external synchronization.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class HookRegistry {

	private static final Logger logger = LoggerFactory.getLogger(HookRegistry.class);

	private final Map<Class<? extends BaseHookEvent>, CopyOnWriteArrayList<HookCallback<? extends BaseHookEvent>>> callbacks = new ConcurrentHashMap<>();

	/**
	 * Register a callback for the specified event type.
	 * @param <T> the event type
	 * @param eventType the class of the event to listen for, must not be {@code null}
	 * @param callback the callback to invoke when an event of the given type is fired,
	 * must not be {@code null}
	 */
	public <T extends BaseHookEvent> void addCallback(Class<T> eventType, HookCallback<T> callback) {
		Assert.notNull(eventType, "eventType must not be null");
		Assert.notNull(callback, "callback must not be null");
		this.callbacks.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(callback);
		logger.debug("Registered callback for event type: {}", eventType.getSimpleName());
	}

	/**
	 * Register all hooks provided by the given {@link HookProvider}.
	 * @param provider the hook provider whose callbacks should be registered, must not be
	 * {@code null}
	 */
	public void addHook(HookProvider provider) {
		Assert.notNull(provider, "provider must not be null");
		provider.registerHooks(this);
		logger.debug("Registered hooks from provider: {}", provider.getClass().getSimpleName());
	}

	/**
	 * Invoke all registered callbacks for the given event.
	 *
	 * <p>
	 * If the event's {@link BaseHookEvent#shouldReverseCallbacks()} returns {@code true},
	 * callbacks are invoked in reverse registration order (LIFO). Otherwise, they are
	 * invoked in registration order (FIFO).
	 *
	 * <p>
	 * The event is returned after all callbacks have been invoked, allowing callers to
	 * inspect any modifications made by the callbacks.
	 * @param <T> the event type
	 * @param event the event to fire, must not be {@code null}
	 * @return the event after all callbacks have been invoked
	 */
	@SuppressWarnings("unchecked")
	public <T extends BaseHookEvent> T invokeCallbacks(T event) {
		Assert.notNull(event, "event must not be null");
		CopyOnWriteArrayList<HookCallback<? extends BaseHookEvent>> registered = this.callbacks.get(event.getClass());
		if (registered == null || registered.isEmpty()) {
			return event;
		}

		List<HookCallback<? extends BaseHookEvent>> orderedCallbacks;
		if (event.shouldReverseCallbacks()) {
			orderedCallbacks = new ArrayList<>(registered);
			Collections.reverse(orderedCallbacks);
		}
		else {
			orderedCallbacks = registered;
		}

		for (HookCallback<? extends BaseHookEvent> callback : orderedCallbacks) {
			try {
				((HookCallback<T>) callback).handle(event);
			}
			catch (Exception ex) {
				logger.error("Error invoking callback for event type: {}", event.getClass().getSimpleName(), ex);
			}
		}

		return event;
	}

	/**
	 * Return whether any callbacks are registered in this registry.
	 * @return {@code true} if at least one callback is registered, {@code false}
	 * otherwise
	 */
	public boolean hasCallbacks() {
		return this.callbacks.values().stream().anyMatch(list -> !list.isEmpty());
	}

	/**
	 * Return whether any callbacks are registered for the specified event type.
	 * @param eventType the event type to check for callbacks
	 * @return {@code true} if at least one callback is registered for the given type,
	 * {@code false} otherwise
	 */
	public boolean hasCallbacks(Class<? extends BaseHookEvent> eventType) {
		CopyOnWriteArrayList<HookCallback<? extends BaseHookEvent>> registered = this.callbacks.get(eventType);
		return registered != null && !registered.isEmpty();
	}

}
