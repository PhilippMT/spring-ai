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

package org.springframework.ai.strands.agent.interrupt;

/**
 * Strategy interface for resolving {@link Interrupt}s in human-in-the-loop scenarios.
 *
 * <p>
 * Implementations determine how to obtain human input for a given interrupt and return
 * the resolved value so that the agent can resume execution.
 *
 * @author Spring AI
 * @since 2.0.0
 * @see Interrupt
 * @see InterruptException
 */
public interface InterruptResolver {

	/**
	 * Resolve the given interrupt by obtaining the necessary input.
	 * @param interrupt the interrupt to resolve, must not be {@code null}
	 * @return the resolved value to continue agent execution
	 */
	Object resolve(Interrupt interrupt);

}
