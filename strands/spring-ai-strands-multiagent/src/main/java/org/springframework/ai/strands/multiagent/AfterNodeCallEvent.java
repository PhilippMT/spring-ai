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

package org.springframework.ai.strands.multiagent;

import org.springframework.ai.strands.hooks.BaseHookEvent;

/**
 * Hook event fired after a node has been called within a multi-agent orchestration.
 *
 * <p>
 * Callbacks for this event are invoked in reverse registration order (LIFO) to ensure
 * proper cleanup semantics.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class AfterNodeCallEvent extends BaseHookEvent {

	private final String nodeId;

	/**
	 * Create a new {@code AfterNodeCallEvent}.
	 * @param nodeId the node identifier
	 */
	public AfterNodeCallEvent(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Return the node identifier.
	 * @return the node identifier
	 */
	public String getNodeId() {
		return this.nodeId;
	}

	@Override
	public boolean shouldReverseCallbacks() {
		return true;
	}

}
