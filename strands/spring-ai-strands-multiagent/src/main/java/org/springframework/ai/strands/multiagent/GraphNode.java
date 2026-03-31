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

import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.strands.agent.Agent;
import org.springframework.util.Assert;

/**
 * Represents a node in a directed graph-based multi-agent orchestration.
 *
 * <p>
 * Each node wraps an {@link Agent} instance and has a unique identifier. An optional
 * input transformer can be applied to modify the input before it reaches the agent.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class GraphNode {

	private final String id;

	private final Agent agent;

	private final @Nullable Function<String, String> inputTransformer;

	/**
	 * Create a new {@code GraphNode} with the given identifier and agent.
	 * @param id the node identifier, must not be {@code null}
	 * @param agent the agent to execute at this node, must not be {@code null}
	 */
	public GraphNode(String id, Agent agent) {
		this(id, agent, null);
	}

	/**
	 * Create a new {@code GraphNode} with the given identifier, agent, and input
	 * transformer.
	 * @param id the node identifier, must not be {@code null}
	 * @param agent the agent to execute at this node, must not be {@code null}
	 * @param inputTransformer optional function to transform the input before execution
	 */
	public GraphNode(String id, Agent agent, @Nullable Function<String, String> inputTransformer) {
		Assert.notNull(id, "id must not be null");
		Assert.notNull(agent, "agent must not be null");
		this.id = id;
		this.agent = agent;
		this.inputTransformer = inputTransformer;
	}

	/**
	 * Return the node identifier.
	 * @return the node identifier
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Return the agent associated with this node.
	 * @return the agent
	 */
	public Agent getAgent() {
		return this.agent;
	}

	/**
	 * Return the input transformer, if any.
	 * @return the input transformer, or {@code null} if none is configured
	 */
	public @Nullable Function<String, String> getInputTransformer() {
		return this.inputTransformer;
	}

	/**
	 * Transform the given input using the configured input transformer, or return the
	 * input unchanged if no transformer is configured.
	 * @param input the input to transform
	 * @return the transformed input
	 */
	public String transformInput(String input) {
		if (this.inputTransformer != null) {
			return this.inputTransformer.apply(input);
		}
		return input;
	}

}
