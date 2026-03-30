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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.strands.hooks.events.AfterInvocationEvent;
import org.springframework.ai.strands.hooks.events.AgentInitializedEvent;
import org.springframework.ai.strands.hooks.events.BeforeInvocationEvent;
import org.springframework.ai.strands.hooks.events.BeforeToolCallEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HookRegistry}.
 *
 * @author Spring AI
 */
class HookRegistryTests {

	private HookRegistry registry;

	@BeforeEach
	void setUp() {
		this.registry = new HookRegistry();
	}

	@Test
	void shouldHaveNoCallbacksWhenEmpty() {
		assertThat(this.registry.hasCallbacks()).isFalse();
	}

	@Test
	void shouldRegisterAndInvokeCallback() {
		List<String> invoked = new ArrayList<>();
		this.registry.addCallback(AgentInitializedEvent.class, event -> invoked.add(event.getAgentId()));

		assertThat(this.registry.hasCallbacks()).isTrue();
		assertThat(this.registry.hasCallbacks(AgentInitializedEvent.class)).isTrue();

		AgentInitializedEvent event = new AgentInitializedEvent("test-agent");
		this.registry.invokeCallbacks(event);

		assertThat(invoked).containsExactly("test-agent");
	}

	@Test
	void shouldInvokeCallbacksInRegistrationOrder() {
		List<Integer> order = new ArrayList<>();
		this.registry.addCallback(AgentInitializedEvent.class, event -> order.add(1));
		this.registry.addCallback(AgentInitializedEvent.class, event -> order.add(2));
		this.registry.addCallback(AgentInitializedEvent.class, event -> order.add(3));

		this.registry.invokeCallbacks(new AgentInitializedEvent("agent"));

		assertThat(order).containsExactly(1, 2, 3);
	}

	@Test
	void shouldInvokeCallbacksInReverseOrderForCleanupEvents() {
		List<Integer> order = new ArrayList<>();
		this.registry.addCallback(AfterInvocationEvent.class, event -> order.add(1));
		this.registry.addCallback(AfterInvocationEvent.class, event -> order.add(2));
		this.registry.addCallback(AfterInvocationEvent.class, event -> order.add(3));

		AfterInvocationEvent event = new AfterInvocationEvent("agent", Collections.emptyMap(), null);
		this.registry.invokeCallbacks(event);

		assertThat(order).containsExactly(3, 2, 1);
	}

	@Test
	void shouldHandleMultipleEventTypes() {
		List<String> results = new ArrayList<>();
		this.registry.addCallback(AgentInitializedEvent.class, event -> results.add("initialized"));
		this.registry.addCallback(BeforeInvocationEvent.class, event -> results.add("before-invocation"));

		this.registry.invokeCallbacks(new AgentInitializedEvent("agent"));
		this.registry.invokeCallbacks(new BeforeInvocationEvent("agent", Collections.emptyMap(), List.of()));

		assertThat(results).containsExactly("initialized", "before-invocation");
	}

	@Test
	void shouldNotInvokeCallbacksForUnregisteredEventTypes() {
		List<String> results = new ArrayList<>();
		this.registry.addCallback(AgentInitializedEvent.class, event -> results.add("initialized"));

		this.registry.invokeCallbacks(new BeforeInvocationEvent("agent", Collections.emptyMap(), List.of()));

		assertThat(results).isEmpty();
	}

	@Test
	void shouldRegisterCallbacksFromProvider() {
		List<String> results = new ArrayList<>();
		HookProvider provider = hookRegistry -> hookRegistry.addCallback(AgentInitializedEvent.class,
				event -> results.add("from-provider"));

		this.registry.addHook(provider);

		assertThat(this.registry.hasCallbacks()).isTrue();
		this.registry.invokeCallbacks(new AgentInitializedEvent("agent"));
		assertThat(results).containsExactly("from-provider");
	}

	@Test
	void shouldReturnEventAfterInvocation() {
		AgentInitializedEvent event = new AgentInitializedEvent("agent");
		AgentInitializedEvent returned = this.registry.invokeCallbacks(event);
		assertThat(returned).isSameAs(event);
	}

	@Test
	void shouldAllowWritableFieldModificationDuringCallback() {
		this.registry.addCallback(BeforeToolCallEvent.class, event -> event.setCancelTool(true));

		BeforeToolCallEvent event = new BeforeToolCallEvent("agent", "myTool", Map.of("key", "value"));
		assertThat(event.isCancelTool()).isFalse();

		this.registry.invokeCallbacks(event);

		assertThat(event.isCancelTool()).isTrue();
	}

	@Test
	void shouldAllowMultipleCallbacksToModifyWritableFields() {
		this.registry.addCallback(AfterInvocationEvent.class, event -> event.setResume(true));
		this.registry.addCallback(AfterInvocationEvent.class, event -> assertThat(event.isResume()).isFalse());

		AfterInvocationEvent event = new AfterInvocationEvent("agent", Collections.emptyMap(), "result");
		this.registry.invokeCallbacks(event);

		assertThat(event.isResume()).isTrue();
	}

	@Test
	void shouldReportHasCallbacksForSpecificEventType() {
		this.registry.addCallback(AgentInitializedEvent.class, event -> {
		});

		assertThat(this.registry.hasCallbacks(AgentInitializedEvent.class)).isTrue();
		assertThat(this.registry.hasCallbacks(BeforeInvocationEvent.class)).isFalse();
	}

	@Test
	void shouldContinueInvocationWhenCallbackThrowsException() {
		List<String> results = new ArrayList<>();
		this.registry.addCallback(AgentInitializedEvent.class, event -> {
			throw new RuntimeException("callback error");
		});
		this.registry.addCallback(AgentInitializedEvent.class, event -> results.add("second"));

		this.registry.invokeCallbacks(new AgentInitializedEvent("agent"));

		assertThat(results).containsExactly("second");
	}

	@Test
	void shouldHandleBeforeInvocationMessageModification() {
		this.registry.addCallback(BeforeInvocationEvent.class, event -> event.setMessages(List.of()));

		BeforeInvocationEvent event = new BeforeInvocationEvent("agent", Collections.emptyMap(), List.of());
		this.registry.invokeCallbacks(event);

		assertThat(event.getMessages()).isEmpty();
	}

}
