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
 * Hook event fired before a node is called within a multi-agent orchestration.
 *
 * <p>
 * Handlers can set {@link #setCancelNode(boolean)} to {@code true} to prevent the node
 * from being executed.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class BeforeNodeCallEvent extends BaseHookEvent {

	private final String nodeId;

	private boolean cancelNode;

	/**
	 * Create a new {@code BeforeNodeCallEvent}.
	 * @param nodeId the node identifier
	 */
	public BeforeNodeCallEvent(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Return the node identifier.
	 * @return the node identifier
	 */
	public String getNodeId() {
		return this.nodeId;
	}

	/**
	 * Return whether this node execution should be cancelled.
	 * @return {@code true} if the node should be cancelled
	 */
	public boolean isCancelNode() {
		return this.cancelNode;
	}

	/**
	 * Set whether this node execution should be cancelled.
	 * @param cancelNode {@code true} to cancel the node
	 */
	public void setCancelNode(boolean cancelNode) {
		this.cancelNode = cancelNode;
	}

}
