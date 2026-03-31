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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.Assert;

/**
 * Abstract base class for multi-agent orchestrators.
 *
 * <p>
 * Provides common infrastructure for executing multi-agent orchestration patterns
 * including timing, error handling, and unique identification.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public abstract class MultiAgentBase {

	private static final Logger logger = LoggerFactory.getLogger(MultiAgentBase.class);

	private final String id;

	/**
	 * Create a new {@code MultiAgentBase} with an auto-generated identifier.
	 */
	protected MultiAgentBase() {
		this.id = UUID.randomUUID().toString();
	}

	/**
	 * Create a new {@code MultiAgentBase} with the given identifier.
	 * @param id the orchestrator identifier, must not be {@code null}
	 */
	protected MultiAgentBase(String id) {
		Assert.notNull(id, "id must not be null");
		this.id = id;
	}

	/**
	 * Return the unique identifier of this orchestrator.
	 * @return the orchestrator identifier
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Invoke the multi-agent orchestration with the given task.
	 * @param task the task description, must not be {@code null}
	 * @param invocationState mutable state passed through the orchestration, must not be
	 * {@code null}
	 * @return the orchestration result
	 */
	public MultiAgentResult invoke(String task, Map<String, Object> invocationState) {
		Assert.notNull(task, "task must not be null");
		Assert.notNull(invocationState, "invocationState must not be null");
		long startTime = System.currentTimeMillis();
		try {
			return invokeInternal(task, invocationState);
		}
		catch (Exception ex) {
			long elapsed = System.currentTimeMillis() - startTime;
			logger.error("Multi-agent orchestration {} failed after {}ms", this.id, elapsed, ex);
			Map<String, NodeResult> errorResults = new HashMap<>();
			errorResults.put("error", NodeResult.fromException(ex, elapsed));
			return new MultiAgentResult(Status.FAILED, errorResults, 0, 0, 0, elapsed, 0, elapsed);
		}
	}

	/**
	 * Invoke the multi-agent orchestration with the given task using an empty invocation
	 * state.
	 * @param task the task description, must not be {@code null}
	 * @return the orchestration result
	 */
	public MultiAgentResult invoke(String task) {
		return invoke(task, new HashMap<>());
	}

	/**
	 * Perform the actual multi-agent orchestration.
	 * @param task the task description
	 * @param invocationState mutable state passed through the orchestration
	 * @return the orchestration result
	 */
	protected abstract MultiAgentResult invokeInternal(String task, Map<String, Object> invocationState);

}
