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

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

import org.springframework.util.StringUtils;

/**
 * Default observation naming conventions for Strands agent operations.
 *
 * <p>
 * Defines the observation name, contextual name, and key-value pairs for agent
 * invocations.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class AgentObservationConvention implements ObservationConvention<AgentObservationContext> {

	public static final String DEFAULT_NAME = "strands.agent.invocation";

	private static final String AGENT_ID_KEY = "agent.id";

	private static final String AGENT_STOP_REASON_KEY = "agent.stop_reason";

	private static final String AGENT_SYSTEM_PROMPT_KEY = "agent.system_prompt";

	private static final KeyValue AGENT_STOP_REASON_NONE = KeyValue.of(AGENT_STOP_REASON_KEY, KeyValue.NONE_VALUE);

	@Override
	public String getName() {
		return DEFAULT_NAME;
	}

	@Override
	public String getContextualName(AgentObservationContext context) {
		return "agent " + context.getAgentId();
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(AgentObservationContext context) {
		return KeyValues.of(agentId(context), stopReason(context));
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(AgentObservationContext context) {
		KeyValues keyValues = KeyValues.empty();
		keyValues = systemPrompt(keyValues, context);
		return keyValues;
	}

	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof AgentObservationContext;
	}

	protected KeyValue agentId(AgentObservationContext context) {
		return KeyValue.of(AGENT_ID_KEY, context.getAgentId());
	}

	protected KeyValue stopReason(AgentObservationContext context) {
		if (StringUtils.hasText(context.getStopReason())) {
			return KeyValue.of(AGENT_STOP_REASON_KEY, context.getStopReason());
		}
		return AGENT_STOP_REASON_NONE;
	}

	protected KeyValues systemPrompt(KeyValues keyValues, AgentObservationContext context) {
		if (StringUtils.hasText(context.getSystemPrompt())) {
			return keyValues.and(AGENT_SYSTEM_PROMPT_KEY, context.getSystemPrompt());
		}
		return keyValues;
	}

}
