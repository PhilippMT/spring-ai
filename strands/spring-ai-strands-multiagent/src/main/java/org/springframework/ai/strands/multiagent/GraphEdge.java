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

import org.springframework.util.Assert;

/**
 * Represents a directed edge between two nodes in a graph-based multi-agent
 * orchestration.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class GraphEdge {

	private final String sourceId;

	private final String targetId;

	/**
	 * Create a new {@code GraphEdge} between the given source and target nodes.
	 * @param sourceId the source node identifier, must not be {@code null}
	 * @param targetId the target node identifier, must not be {@code null}
	 */
	public GraphEdge(String sourceId, String targetId) {
		Assert.notNull(sourceId, "sourceId must not be null");
		Assert.notNull(targetId, "targetId must not be null");
		this.sourceId = sourceId;
		this.targetId = targetId;
	}

	/**
	 * Return the source node identifier.
	 * @return the source node identifier
	 */
	public String getSourceId() {
		return this.sourceId;
	}

	/**
	 * Return the target node identifier.
	 * @return the target node identifier
	 */
	public String getTargetId() {
		return this.targetId;
	}

}
