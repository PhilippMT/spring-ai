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

package org.springframework.ai.strands.agent.loop;

/**
 * Exception thrown when the agent event loop exceeds its configured maximum number of
 * iterations.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class MaxIterationsExceededException extends RuntimeException {

	private final int maxIterations;

	/**
	 * Create a new {@code MaxIterationsExceededException}.
	 * @param maxIterations the configured maximum number of iterations
	 */
	public MaxIterationsExceededException(int maxIterations) {
		super("Agent event loop exceeded the maximum number of iterations: " + maxIterations);
		this.maxIterations = maxIterations;
	}

	/**
	 * Return the configured maximum number of iterations.
	 * @return the maximum iterations
	 */
	public int getMaxIterations() {
		return this.maxIterations;
	}

}
