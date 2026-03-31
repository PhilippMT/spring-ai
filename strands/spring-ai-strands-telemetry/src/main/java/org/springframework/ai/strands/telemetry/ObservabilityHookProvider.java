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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.strands.hooks.HookProvider;
import org.springframework.ai.strands.hooks.HookRegistry;
import org.springframework.ai.strands.hooks.events.AfterInvocationEvent;
import org.springframework.ai.strands.hooks.events.AfterToolCallEvent;
import org.springframework.ai.strands.hooks.events.BeforeInvocationEvent;
import org.springframework.ai.strands.hooks.events.BeforeToolCallEvent;
import org.springframework.util.Assert;

/**
 * A {@link HookProvider} that integrates with Micrometer Observation to provide
 * observability for Strands agent invocations and tool calls.
 *
 * <p>
 * Starts an {@link Observation} on {@link BeforeInvocationEvent} and stops it on
 * {@link AfterInvocationEvent}. Similarly, starts a child observation on
 * {@link BeforeToolCallEvent} and stops it on {@link AfterToolCallEvent}.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class ObservabilityHookProvider implements HookProvider {

	private static final Logger logger = LoggerFactory.getLogger(ObservabilityHookProvider.class);

	private static final String TOOL_OBSERVATION_NAME = "strands.agent.tool";

	private final ObservationRegistry observationRegistry;

	private final AgentObservationConvention convention;

	private final Map<String, Observation> activeAgentObservations = new ConcurrentHashMap<>();

	private final Map<String, Observation> activeToolObservations = new ConcurrentHashMap<>();

	public ObservabilityHookProvider(ObservationRegistry observationRegistry) {
		this(observationRegistry, new AgentObservationConvention());
	}

	public ObservabilityHookProvider(ObservationRegistry observationRegistry, AgentObservationConvention convention) {
		Assert.notNull(observationRegistry, "observationRegistry must not be null");
		Assert.notNull(convention, "convention must not be null");
		this.observationRegistry = observationRegistry;
		this.convention = convention;
	}

	@Override
	public void registerHooks(HookRegistry registry) {
		Assert.notNull(registry, "registry must not be null");
		registry.addCallback(BeforeInvocationEvent.class, this::onBeforeInvocation);
		registry.addCallback(AfterInvocationEvent.class, this::onAfterInvocation);
		registry.addCallback(BeforeToolCallEvent.class, this::onBeforeToolCall);
		registry.addCallback(AfterToolCallEvent.class, this::onAfterToolCall);
	}

	private void onBeforeInvocation(BeforeInvocationEvent event) {
		AgentObservationContext context = new AgentObservationContext(event.getAgentId());
		Observation observation = Observation.createNotStarted(this.convention, () -> context, this.observationRegistry)
			.start();
		this.activeAgentObservations.put(event.getAgentId(), observation);
		logger.debug("Started observation for agent: {}", event.getAgentId());
	}

	private void onAfterInvocation(AfterInvocationEvent event) {
		Observation observation = this.activeAgentObservations.remove(event.getAgentId());
		if (observation != null) {
			observation.stop();
			logger.debug("Stopped observation for agent: {}", event.getAgentId());
		}
	}

	private void onBeforeToolCall(BeforeToolCallEvent event) {
		String toolKey = event.getAgentId() + ":" + event.getToolName();
		Observation parentObservation = this.activeAgentObservations.get(event.getAgentId());
		Observation observation = Observation.createNotStarted(TOOL_OBSERVATION_NAME, this.observationRegistry)
			.parentObservation(parentObservation)
			.lowCardinalityKeyValue("tool.name", event.getToolName())
			.lowCardinalityKeyValue("agent.id", event.getAgentId())
			.start();
		this.activeToolObservations.put(toolKey, observation);
		logger.debug("Started tool observation for tool: {} in agent: {}", event.getToolName(), event.getAgentId());
	}

	private void onAfterToolCall(AfterToolCallEvent event) {
		String toolKey = event.getAgentId() + ":" + event.getToolName();
		Observation observation = this.activeToolObservations.remove(toolKey);
		if (observation != null) {
			if (event.getException() != null) {
				observation.error(event.getException());
			}
			observation.stop();
			logger.debug("Stopped tool observation for tool: {} in agent: {}", event.getToolName(), event.getAgentId());
		}
	}

}
