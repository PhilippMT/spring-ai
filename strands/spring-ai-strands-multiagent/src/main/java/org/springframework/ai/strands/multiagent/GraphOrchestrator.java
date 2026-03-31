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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.strands.agent.AgentResult;

/**
 * Directed graph-based multi-agent orchestrator.
 *
 * <p>
 * Executes agents as nodes in a directed acyclic graph (DAG), following topological
 * order. Independent nodes at the same level are executed in parallel using
 * {@link CompletableFuture}. Output from source nodes is passed as input to connected
 * target nodes.
 *
 * <p>
 * The graph is validated during construction to ensure it is acyclic and that all
 * referenced nodes exist. Use {@link GraphBuilder} to construct instances.
 *
 * @author Spring AI
 * @since 2.0.0
 * @see GraphBuilder
 * @see GraphNode
 * @see GraphEdge
 */
public class GraphOrchestrator extends MultiAgentBase {

	private static final Logger logger = LoggerFactory.getLogger(GraphOrchestrator.class);

	private final Map<String, GraphNode> nodes;

	private final List<GraphEdge> edges;

	private final String entryPoint;

	/**
	 * Create a new {@code GraphOrchestrator}. Use {@link GraphBuilder} to construct
	 * instances.
	 * @param nodes the graph nodes keyed by identifier
	 * @param edges the directed edges between nodes
	 * @param entryPoint the entry point node identifier
	 */
	GraphOrchestrator(Map<String, GraphNode> nodes, List<GraphEdge> edges, String entryPoint) {
		this.nodes = nodes;
		this.edges = edges;
		this.entryPoint = entryPoint;
		validateAcyclic();
	}

	@Override
	protected MultiAgentResult invokeInternal(String task, Map<String, Object> invocationState) {
		long startTime = System.currentTimeMillis();
		Map<String, NodeResult> results = new LinkedHashMap<>();
		Map<String, String> nodeOutputs = new ConcurrentHashMap<>();

		List<List<String>> executionLevels = computeTopologicalLevels();
		logger.debug("Graph orchestrator {} executing {} levels for task", getId(), executionLevels.size());

		long totalInputTokens = 0;
		long totalOutputTokens = 0;
		long totalTokens = 0;
		long totalLatency = 0;
		int totalExecutionCount = 0;
		Status overallStatus = Status.COMPLETED;

		for (List<String> level : executionLevels) {
			if (level.size() == 1) {
				String nodeId = level.get(0);
				NodeResult nodeResult = executeNode(nodeId, task, nodeOutputs, invocationState);
				results.put(nodeId, nodeResult);
				totalInputTokens += nodeResult.getInputTokens();
				totalOutputTokens += nodeResult.getOutputTokens();
				totalTokens += nodeResult.getTotalTokens();
				totalLatency += nodeResult.getLatencyMs();
				totalExecutionCount += nodeResult.getExecutionCount();
				if (nodeResult.getStatus() == Status.FAILED) {
					overallStatus = Status.FAILED;
				}
			}
			else {
				List<CompletableFuture<Void>> futures = new ArrayList<>();
				Map<String, NodeResult> levelResults = new ConcurrentHashMap<>();

				for (String nodeId : level) {
					CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
						NodeResult nodeResult = executeNode(nodeId, task, nodeOutputs, invocationState);
						levelResults.put(nodeId, nodeResult);
					});
					futures.add(future);
				}

				CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

				for (String nodeId : level) {
					NodeResult nodeResult = Objects.requireNonNull(levelResults.get(nodeId));
					results.put(nodeId, nodeResult);
					totalInputTokens += nodeResult.getInputTokens();
					totalOutputTokens += nodeResult.getOutputTokens();
					totalTokens += nodeResult.getTotalTokens();
					totalLatency += nodeResult.getLatencyMs();
					totalExecutionCount += nodeResult.getExecutionCount();
					if (nodeResult.getStatus() == Status.FAILED) {
						overallStatus = Status.FAILED;
					}
				}
			}
		}

		long executionTimeMs = System.currentTimeMillis() - startTime;
		return new MultiAgentResult(overallStatus, results, totalInputTokens, totalOutputTokens, totalTokens,
				totalLatency, totalExecutionCount, executionTimeMs);
	}

	private NodeResult executeNode(String nodeId, String task, Map<String, String> nodeOutputs,
			Map<String, Object> invocationState) {
		GraphNode node = Objects.requireNonNull(this.nodes.get(nodeId));
		long nodeStart = System.currentTimeMillis();

		String input = buildNodeInput(nodeId, task, nodeOutputs);
		input = node.transformInput(input);

		try {
			logger.debug("Executing graph node '{}'", nodeId);
			AgentResult agentResult = node.getAgent().call(input, invocationState);
			long elapsed = System.currentTimeMillis() - nodeStart;

			String output = agentResult.getMessage().getText();
			nodeOutputs.put(nodeId, output);

			return NodeResult.fromAgentResult(agentResult, elapsed);
		}
		catch (Exception ex) {
			long elapsed = System.currentTimeMillis() - nodeStart;
			logger.error("Graph node '{}' failed after {}ms", nodeId, elapsed, ex);
			nodeOutputs.put(nodeId, "");
			return NodeResult.fromException(ex, elapsed);
		}
	}

	private String buildNodeInput(String nodeId, String originalTask, Map<String, String> nodeOutputs) {
		List<String> parentIds = this.edges.stream()
			.filter(e -> e.getTargetId().equals(nodeId))
			.map(GraphEdge::getSourceId)
			.toList();

		if (parentIds.isEmpty()) {
			return originalTask;
		}

		StringBuilder sb = new StringBuilder();
		for (String parentId : parentIds) {
			String parentOutput = nodeOutputs.get(parentId);
			if (parentOutput != null && !parentOutput.isEmpty()) {
				sb.append(parentOutput).append("\n");
			}
		}
		return sb.toString().trim();
	}

	private List<List<String>> computeTopologicalLevels() {
		Map<String, Integer> inDegree = new HashMap<>();
		Map<String, List<String>> adjacency = new HashMap<>();

		for (String nodeId : this.nodes.keySet()) {
			inDegree.put(nodeId, 0);
			adjacency.put(nodeId, new ArrayList<>());
		}

		for (GraphEdge edge : this.edges) {
			Objects.requireNonNull(adjacency.get(edge.getSourceId())).add(edge.getTargetId());
			inDegree.merge(edge.getTargetId(), 1, Integer::sum);
		}

		Queue<String> queue = new LinkedList<>();
		for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
			if (entry.getValue() == 0) {
				queue.add(entry.getKey());
			}
		}

		List<List<String>> levels = new ArrayList<>();
		while (!queue.isEmpty()) {
			List<String> currentLevel = new ArrayList<>(queue);
			queue.clear();
			levels.add(currentLevel);

			for (String nodeId : currentLevel) {
				for (String neighbor : Objects.requireNonNull(adjacency.get(nodeId))) {
					inDegree.merge(neighbor, -1, Integer::sum);
					if (Objects.requireNonNull(inDegree.get(neighbor)) == 0) {
						queue.add(neighbor);
					}
				}
			}
		}

		return levels;
	}

	private void validateAcyclic() {
		Map<String, Integer> inDegree = new HashMap<>();
		Map<String, List<String>> adjacency = new HashMap<>();

		for (String nodeId : this.nodes.keySet()) {
			inDegree.put(nodeId, 0);
			adjacency.put(nodeId, new ArrayList<>());
		}

		for (GraphEdge edge : this.edges) {
			Objects.requireNonNull(adjacency.get(edge.getSourceId())).add(edge.getTargetId());
			inDegree.merge(edge.getTargetId(), 1, Integer::sum);
		}

		Queue<String> queue = new LinkedList<>();
		for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
			if (entry.getValue() == 0) {
				queue.add(entry.getKey());
			}
		}

		int visited = 0;
		while (!queue.isEmpty()) {
			String nodeId = queue.poll();
			visited++;
			for (String neighbor : Objects.requireNonNull(adjacency.get(nodeId))) {
				inDegree.merge(neighbor, -1, Integer::sum);
				if (Objects.requireNonNull(inDegree.get(neighbor)) == 0) {
					queue.add(neighbor);
				}
			}
		}

		if (visited != this.nodes.size()) {
			throw new IllegalStateException(
					"Graph contains a cycle; only directed acyclic graphs (DAGs) are supported");
		}
	}

	/**
	 * Return the graph nodes.
	 * @return an unmodifiable view of the nodes
	 */
	public Map<String, GraphNode> getNodes() {
		return Map.copyOf(this.nodes);
	}

	/**
	 * Return the graph edges.
	 * @return an unmodifiable copy of the edges
	 */
	public List<GraphEdge> getEdges() {
		return List.copyOf(this.edges);
	}

	/**
	 * Return the entry point node identifier.
	 * @return the entry point
	 */
	public String getEntryPoint() {
		return this.entryPoint;
	}

}
