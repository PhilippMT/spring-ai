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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.strands.agent.Agent;
import org.springframework.util.Assert;

/**
 * Builder for constructing graph-based multi-agent orchestration systems.
 *
 * <p>
 * Allows defining nodes (agents) and directed edges between them to create a
 * {@link GraphOrchestrator} that executes agents in topological order.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class GraphBuilder {

	private final Map<String, GraphNode> nodes = new LinkedHashMap<>();

	private final List<GraphEdge> edges = new ArrayList<>();

	private @Nullable String entryPoint;

	/**
	 * Add a node to the graph with the given identifier and agent.
	 * @param id the node identifier, must not be {@code null}
	 * @param agent the agent for this node, must not be {@code null}
	 * @return this builder for chaining
	 */
	public GraphBuilder addNode(String id, Agent agent) {
		Assert.notNull(id, "id must not be null");
		Assert.notNull(agent, "agent must not be null");
		this.nodes.put(id, new GraphNode(id, agent));
		return this;
	}

	/**
	 * Add a node to the graph with the given identifier, agent, and input transformer.
	 * @param id the node identifier, must not be {@code null}
	 * @param agent the agent for this node, must not be {@code null}
	 * @param inputTransformer function to transform the input before execution
	 * @return this builder for chaining
	 */
	public GraphBuilder addNode(String id, Agent agent, Function<String, String> inputTransformer) {
		Assert.notNull(id, "id must not be null");
		Assert.notNull(agent, "agent must not be null");
		Assert.notNull(inputTransformer, "inputTransformer must not be null");
		this.nodes.put(id, new GraphNode(id, agent, inputTransformer));
		return this;
	}

	/**
	 * Add a directed edge between two nodes.
	 * @param sourceId the source node identifier, must not be {@code null}
	 * @param targetId the target node identifier, must not be {@code null}
	 * @return this builder for chaining
	 */
	public GraphBuilder addEdge(String sourceId, String targetId) {
		Assert.notNull(sourceId, "sourceId must not be null");
		Assert.notNull(targetId, "targetId must not be null");
		this.edges.add(new GraphEdge(sourceId, targetId));
		return this;
	}

	/**
	 * Set the entry point node for graph execution.
	 * @param nodeId the entry point node identifier, must not be {@code null}
	 * @return this builder for chaining
	 */
	public GraphBuilder setEntryPoint(String nodeId) {
		Assert.notNull(nodeId, "nodeId must not be null");
		this.entryPoint = nodeId;
		return this;
	}

	/**
	 * Build the {@link GraphOrchestrator} from the configured nodes and edges.
	 * @return a new graph orchestrator
	 * @throws IllegalStateException if no entry point is set or if validation fails
	 */
	public GraphOrchestrator build() {
		Assert.state(this.entryPoint != null, "Entry point must be set");
		Assert.state(this.nodes.containsKey(this.entryPoint),
				"Entry point '" + this.entryPoint + "' must refer to an existing node");
		for (GraphEdge edge : this.edges) {
			Assert.state(this.nodes.containsKey(edge.getSourceId()),
					"Edge source '" + edge.getSourceId() + "' must refer to an existing node");
			Assert.state(this.nodes.containsKey(edge.getTargetId()),
					"Edge target '" + edge.getTargetId() + "' must refer to an existing node");
		}
		return new GraphOrchestrator(new LinkedHashMap<>(this.nodes), new ArrayList<>(this.edges), this.entryPoint);
	}

}
