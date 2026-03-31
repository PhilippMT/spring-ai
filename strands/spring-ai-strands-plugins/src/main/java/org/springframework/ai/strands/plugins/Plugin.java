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

package org.springframework.ai.strands.plugins;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.strands.hooks.BaseHookEvent;
import org.springframework.ai.strands.hooks.HookCallback;
import org.springframework.ai.tool.ToolCallback;

/**
 * Base class for plugins that extend agent functionality. Subclasses declare hook
 * callbacks via {@link PluginHook} and tool callbacks via {@link PluginTool}. These
 * annotated methods are auto-discovered during construction and exposed through
 * {@link #getHooks()} and {@link #getTools()}.
 *
 * <p>
 * Subclasses must implement {@link #getName()} and may override
 * {@link #initAgent(Object)} to perform custom initialization when the plugin is attached
 * to an agent.
 *
 * @author Spring AI
 * @since 2.0.0
 * @see PluginHook
 * @see PluginTool
 * @see PluginRegistry
 */
public abstract class Plugin {

	private static final Logger logger = LoggerFactory.getLogger(Plugin.class);

	private final List<HookCallback<?>> hooks;

	private final List<ToolCallback> tools;

	protected Plugin() {
		List<HookCallback<?>> discoveredHooks = new ArrayList<>();
		List<ToolCallback> discoveredTools = new ArrayList<>();
		discoverAnnotatedMethods(discoveredHooks, discoveredTools);
		this.hooks = Collections.unmodifiableList(discoveredHooks);
		this.tools = Collections.unmodifiableList(discoveredTools);
		logger.debug("Plugin '{}' discovered {} hooks and {} tools", getName(), this.hooks.size(), this.tools.size());
	}

	/**
	 * Return the unique name of this plugin.
	 * @return the plugin name, never {@code null}
	 */
	public abstract String getName();

	/**
	 * Return the auto-discovered hook callbacks declared via {@link PluginHook}.
	 * @return an unmodifiable list of hook callbacks
	 */
	public List<HookCallback<?>> getHooks() {
		return this.hooks;
	}

	/**
	 * Return the auto-discovered tool callbacks declared via {@link PluginTool}.
	 * @return an unmodifiable list of tool callbacks
	 */
	public List<ToolCallback> getTools() {
		return this.tools;
	}

	/**
	 * Called when the plugin is attached to an agent. Override to perform custom
	 * initialization.
	 * @param agent the agent instance
	 */
	public void initAgent(Object agent) {
	}

	@SuppressWarnings("unchecked")
	private void discoverAnnotatedMethods(List<HookCallback<?>> hookList, List<ToolCallback> toolList) {
		for (Method method : getClass().getDeclaredMethods()) {
			if (method.isAnnotationPresent(PluginHook.class)) {
				discoverHookMethod(method, hookList);
			}
			if (method.isAnnotationPresent(PluginTool.class)) {
				discoverToolMethod(method, toolList);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void discoverHookMethod(Method method, List<HookCallback<?>> hookList) {
		PluginHook annotation = method.getAnnotation(PluginHook.class);
		Class<? extends BaseHookEvent>[] eventTypes = annotation.value();

		if (eventTypes.length == 0) {
			if (method.getParameterCount() != 1
					|| !BaseHookEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
				logger.warn("@PluginHook method '{}' in plugin '{}' must accept a single BaseHookEvent parameter "
						+ "when no event type is specified", method.getName(), getName());
				return;
			}
			eventTypes = new Class[] { (Class<? extends BaseHookEvent>) method.getParameterTypes()[0] };
		}

		method.setAccessible(true);
		for (Class<? extends BaseHookEvent> eventType : eventTypes) {
			hookList.add(new AnnotatedHookCallback<>(this, method, eventType));
		}
	}

	private void discoverToolMethod(Method method, List<ToolCallback> toolList) {
		if (method.getParameterCount() != 0 || !ToolCallback.class.isAssignableFrom(method.getReturnType())) {
			logger.warn("@PluginTool method '{}' in plugin '{}' must accept no parameters and return ToolCallback",
					method.getName(), getName());
			return;
		}

		method.setAccessible(true);
		try {
			ToolCallback tool = (ToolCallback) method.invoke(this);
			if (tool != null) {
				toolList.add(tool);
			}
		}
		catch (IllegalAccessException | InvocationTargetException ex) {
			logger.error("Failed to invoke @PluginTool method '{}' in plugin '{}'", method.getName(), getName(), ex);
		}
	}

	/**
	 * Adapter that wraps an annotated method as a {@link HookCallback}.
	 */
	static final class AnnotatedHookCallback<T extends BaseHookEvent> implements HookCallback<T> {

		private final Plugin plugin;

		private final Method method;

		private final Class<T> eventType;

		AnnotatedHookCallback(Plugin plugin, Method method, Class<T> eventType) {
			this.plugin = plugin;
			this.method = method;
			this.eventType = eventType;
		}

		@Override
		public void handle(T event) {
			try {
				this.method.invoke(this.plugin, event);
			}
			catch (IllegalAccessException | InvocationTargetException ex) {
				logger.error("Error invoking @PluginHook method '{}' in plugin '{}'", this.method.getName(),
						this.plugin.getName(), ex);
			}
		}

		Class<T> getEventType() {
			return this.eventType;
		}

	}

}
