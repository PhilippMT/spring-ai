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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.strands.hooks.BaseHookEvent;
import org.springframework.ai.strands.hooks.HookRegistry;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PluginRegistry}.
 *
 * @author Spring AI
 */
class PluginRegistryTests {

	private PluginRegistry registry;

	private HookRegistry hookRegistry;

	@BeforeEach
	void setUp() {
		this.registry = new PluginRegistry();
		this.hookRegistry = new HookRegistry();
	}

	@Test
	void addPluginRegistersHooks() {
		AtomicBoolean hookInvoked = new AtomicBoolean(false);
		Plugin plugin = new TestHookPlugin("test-plugin", hookInvoked);

		this.registry.addAndInit(plugin, this.hookRegistry);

		assertThat(this.registry.contains("test-plugin")).isTrue();
		assertThat(this.registry.size()).isEqualTo(1);

		this.hookRegistry.invokeCallbacks(new TestEvent());
		assertThat(hookInvoked.get()).isTrue();
	}

	@Test
	void duplicateNameThrows() {
		Plugin plugin1 = new EmptyPlugin("duplicate");
		Plugin plugin2 = new EmptyPlugin("duplicate");

		this.registry.addAndInit(plugin1, this.hookRegistry);

		assertThatThrownBy(() -> this.registry.addAndInit(plugin2, this.hookRegistry))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("duplicate");
	}

	@Test
	void pluginWithNoHooksOrTools() {
		Plugin plugin = new EmptyPlugin("empty");

		this.registry.addAndInit(plugin, this.hookRegistry);

		assertThat(this.registry.contains("empty")).isTrue();
		assertThat(plugin.getHooks()).isEmpty();
		assertThat(plugin.getTools()).isEmpty();
	}

	@Test
	void pluginInitializationCallback() {
		AtomicReference<Object> capturedAgent = new AtomicReference<>();
		Plugin plugin = new InitPlugin("init-plugin", capturedAgent);

		Object fakeAgent = new Object();
		plugin.initAgent(fakeAgent);

		assertThat(capturedAgent.get()).isSameAs(fakeAgent);
	}

	@Test
	void multiplePlugins() {
		Plugin plugin1 = new EmptyPlugin("plugin-1");
		Plugin plugin2 = new EmptyPlugin("plugin-2");
		Plugin plugin3 = new EmptyPlugin("plugin-3");

		this.registry.addAndInit(plugin1, this.hookRegistry);
		this.registry.addAndInit(plugin2, this.hookRegistry);
		this.registry.addAndInit(plugin3, this.hookRegistry);

		assertThat(this.registry.size()).isEqualTo(3);
		assertThat(this.registry.contains("plugin-1")).isTrue();
		assertThat(this.registry.contains("plugin-2")).isTrue();
		assertThat(this.registry.contains("plugin-3")).isTrue();
	}

	@Test
	void getReturnsPluginByName() {
		Plugin plugin = new EmptyPlugin("named");

		this.registry.addAndInit(plugin, this.hookRegistry);

		assertThat(this.registry.get("named")).isSameAs(plugin);
		assertThat(this.registry.get("nonexistent")).isNull();
	}

	@Test
	void pluginWithToolCallback() {
		Plugin plugin = new ToolPlugin("tool-plugin");

		this.registry.addAndInit(plugin, this.hookRegistry);

		assertThat(plugin.getTools()).hasSize(1);
		assertThat(plugin.getTools().get(0).getToolDefinition().name()).isEqualTo("test-tool");
	}

	static class TestEvent extends BaseHookEvent {

	}

	static class TestHookPlugin extends Plugin {

		private final String name;

		private final AtomicBoolean hookInvoked;

		TestHookPlugin(String name, AtomicBoolean hookInvoked) {
			this.name = name;
			this.hookInvoked = hookInvoked;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@PluginHook
		void onTestEvent(TestEvent event) {
			this.hookInvoked.set(true);
		}

	}

	static class EmptyPlugin extends Plugin {

		private final String name;

		EmptyPlugin(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

	}

	static class InitPlugin extends Plugin {

		private final String name;

		private final AtomicReference<Object> capturedAgent;

		InitPlugin(String name, AtomicReference<Object> capturedAgent) {
			this.name = name;
			this.capturedAgent = capturedAgent;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public void initAgent(Object agent) {
			this.capturedAgent.set(agent);
		}

	}

	static class ToolPlugin extends Plugin {

		private final String name;

		ToolPlugin(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@PluginTool
		ToolCallback testTool() {
			return new ToolCallback() {
				@Override
				public ToolDefinition getToolDefinition() {
					return ToolDefinition.builder()
						.name("test-tool")
						.description("A test tool")
						.inputSchema("{\"type\":\"object\",\"properties\":{}}")
						.build();
				}

				@Override
				public String call(String toolInput) {
					return "result";
				}
			};
		}

	}

}
