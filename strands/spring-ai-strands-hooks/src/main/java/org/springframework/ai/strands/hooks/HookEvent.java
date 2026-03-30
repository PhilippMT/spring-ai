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

import org.springframework.util.Assert;

/**
 * Base class for hook events scoped to a single agent.
 *
 * <p>
 * Extends {@link BaseHookEvent} with an agent identifier so that callbacks can determine
 * which agent triggered the event. The agent identifier is a simple {@link String} to
 * avoid circular dependencies with the agent module.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public abstract class HookEvent extends BaseHookEvent {

	private final String agentId;

	/**
	 * Create a new {@code HookEvent} for the given agent.
	 * @param agentId the identifier of the agent that triggered the event, must not be
	 * {@code null}
	 */
	protected HookEvent(String agentId) {
		Assert.notNull(agentId, "agentId must not be null");
		this.agentId = agentId;
	}

	/**
	 * Return the identifier of the agent that triggered this event.
	 * @return the agent identifier, never {@code null}
	 */
	public String getAgentId() {
		return this.agentId;
	}

}
