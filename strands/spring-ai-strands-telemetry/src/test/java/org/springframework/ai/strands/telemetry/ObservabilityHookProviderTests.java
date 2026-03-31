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

package org.springframework.ai.strands.telemetry;

import java.util.Collections;
import java.util.Map;

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.strands.hooks.HookRegistry;
import org.springframework.ai.strands.hooks.events.AfterInvocationEvent;
import org.springframework.ai.strands.hooks.events.AfterToolCallEvent;
import org.springframework.ai.strands.hooks.events.BeforeInvocationEvent;
import org.springframework.ai.strands.hooks.events.BeforeToolCallEvent;

/**
 * Tests for {@link ObservabilityHookProvider}.
 *
 * @author Spring AI
 * @since 2.0.0
 */
class ObservabilityHookProviderTests {

	private TestObservationRegistry observationRegistry;

	private HookRegistry hookRegistry;

	@BeforeEach
	void setUp() {
		this.observationRegistry = TestObservationRegistry.create();
		this.hookRegistry = new HookRegistry();
		ObservabilityHookProvider provider = new ObservabilityHookProvider(this.observationRegistry);
		provider.registerHooks(this.hookRegistry);
	}

	@Test
	void beforeInvocationStartsObservation() {
		BeforeInvocationEvent event = new BeforeInvocationEvent("test-agent", Collections.emptyMap(),
				Collections.emptyList());

		this.hookRegistry.invokeCallbacks(event);

		TestObservationRegistryAssert.assertThat(this.observationRegistry)
			.hasObservationWithNameEqualTo("strands.agent.invocation")
			.that()
			.hasBeenStarted()
			.isNotStopped();
	}

	@Test
	void afterInvocationStopsObservation() {
		BeforeInvocationEvent beforeEvent = new BeforeInvocationEvent("test-agent", Collections.emptyMap(),
				Collections.emptyList());
		this.hookRegistry.invokeCallbacks(beforeEvent);

		AfterInvocationEvent afterEvent = new AfterInvocationEvent("test-agent", Collections.emptyMap(), "result");
		this.hookRegistry.invokeCallbacks(afterEvent);

		TestObservationRegistryAssert.assertThat(this.observationRegistry)
			.hasObservationWithNameEqualTo("strands.agent.invocation")
			.that()
			.hasBeenStopped();
	}

	@Test
	void observationRecordsAgentContext() {
		BeforeInvocationEvent beforeEvent = new BeforeInvocationEvent("my-agent", Collections.emptyMap(),
				Collections.emptyList());
		this.hookRegistry.invokeCallbacks(beforeEvent);

		AfterInvocationEvent afterEvent = new AfterInvocationEvent("my-agent", Collections.emptyMap(), "result");
		this.hookRegistry.invokeCallbacks(afterEvent);

		TestObservationRegistryAssert.assertThat(this.observationRegistry)
			.hasObservationWithNameEqualTo("strands.agent.invocation")
			.that()
			.hasBeenStopped()
			.hasLowCardinalityKeyValue("agent.id", "my-agent")
			.hasContextualNameEqualTo("agent my-agent");
	}

	@Test
	void toolCallObservationsAreRecorded() {
		BeforeInvocationEvent beforeInvocation = new BeforeInvocationEvent("test-agent", Collections.emptyMap(),
				Collections.emptyList());
		this.hookRegistry.invokeCallbacks(beforeInvocation);

		Map<String, Object> toolInput = Collections.singletonMap("query", "test");
		BeforeToolCallEvent beforeTool = new BeforeToolCallEvent("test-agent", "search-tool", toolInput);
		this.hookRegistry.invokeCallbacks(beforeTool);

		AfterToolCallEvent afterTool = new AfterToolCallEvent("test-agent", "search-tool", "tool-result", null);
		this.hookRegistry.invokeCallbacks(afterTool);

		AfterInvocationEvent afterInvocation = new AfterInvocationEvent("test-agent", Collections.emptyMap(), "result");
		this.hookRegistry.invokeCallbacks(afterInvocation);

		TestObservationRegistryAssert.assertThat(this.observationRegistry)
			.hasObservationWithNameEqualTo("strands.agent.tool")
			.that()
			.hasBeenStopped()
			.hasLowCardinalityKeyValue("tool.name", "search-tool")
			.hasLowCardinalityKeyValue("agent.id", "test-agent");
	}

	@Test
	void toolCallErrorIsRecordedOnObservation() {
		BeforeInvocationEvent beforeInvocation = new BeforeInvocationEvent("test-agent", Collections.emptyMap(),
				Collections.emptyList());
		this.hookRegistry.invokeCallbacks(beforeInvocation);

		Map<String, Object> toolInput = Collections.singletonMap("query", "test");
		BeforeToolCallEvent beforeTool = new BeforeToolCallEvent("test-agent", "failing-tool", toolInput);
		this.hookRegistry.invokeCallbacks(beforeTool);

		RuntimeException exception = new RuntimeException("Tool failed");
		AfterToolCallEvent afterTool = new AfterToolCallEvent("test-agent", "failing-tool", null, exception);
		this.hookRegistry.invokeCallbacks(afterTool);

		AfterInvocationEvent afterInvocation = new AfterInvocationEvent("test-agent", Collections.emptyMap(), "result");
		this.hookRegistry.invokeCallbacks(afterInvocation);

		TestObservationRegistryAssert.assertThat(this.observationRegistry)
			.hasObservationWithNameEqualTo("strands.agent.tool")
			.that()
			.hasBeenStopped()
			.hasLowCardinalityKeyValue("tool.name", "failing-tool")
			.hasError();
	}

}
