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

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.strands.hooks.BaseHookEvent;
import org.springframework.ai.strands.hooks.HookCallback;
import org.springframework.ai.strands.hooks.HookRegistry;
import org.springframework.util.Assert;

/**
 * Manages a set of {@link Plugin} instances attached to an agent. Each plugin is
 * registered by name and its hooks and tools are wired into the agent's lifecycle.
 * Duplicate plugin names are rejected to prevent configuration errors.
 *
 * @author Spring AI
 * @since 2.0.0
 * @see Plugin
 * @see HookRegistry
 */
public class PluginRegistry {

	private static final Logger logger = LoggerFactory.getLogger(PluginRegistry.class);

	private final Map<String, Plugin> plugins = new LinkedHashMap<>();

	/**
	 * Add a plugin and initialize it. The plugin's hooks are registered with the given
	 * {@link HookRegistry} and its tools are made available for agent use.
	 * @param plugin the plugin to register, must not be {@code null}
	 * @param hookRegistry the hook registry to register the plugin's hooks with, must not
	 * be {@code null}
	 * @throws IllegalArgumentException if a plugin with the same name is already
	 * registered
	 */
	@SuppressWarnings("unchecked")
	public void addAndInit(Plugin plugin, HookRegistry hookRegistry) {
		Assert.notNull(plugin, "plugin must not be null");
		Assert.notNull(hookRegistry, "hookRegistry must not be null");

		String name = plugin.getName();
		if (this.plugins.containsKey(name)) {
			throw new IllegalArgumentException("Plugin with name '" + name + "' is already registered");
		}

		for (HookCallback<?> hook : plugin.getHooks()) {
			if (hook instanceof Plugin.AnnotatedHookCallback<?> annotatedCallback) {
				hookRegistry.addCallback((Class<BaseHookEvent>) annotatedCallback.getEventType(),
						(HookCallback<BaseHookEvent>) annotatedCallback);
			}
		}

		this.plugins.put(name, plugin);
		logger.debug("Registered plugin '{}' with {} hooks and {} tools", name, plugin.getHooks().size(),
				plugin.getTools().size());
	}

	/**
	 * Return whether a plugin with the given name is registered.
	 * @param name the plugin name
	 * @return {@code true} if a plugin with that name exists
	 */
	public boolean contains(String name) {
		return this.plugins.containsKey(name);
	}

	/**
	 * Return the number of registered plugins.
	 * @return the plugin count
	 */
	public int size() {
		return this.plugins.size();
	}

	/**
	 * Return the plugin registered with the given name, or {@code null} if not found.
	 * @param name the plugin name
	 * @return the plugin or {@code null}
	 */
	public @Nullable Plugin get(String name) {
		return this.plugins.get(name);
	}

}
