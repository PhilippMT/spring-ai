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

import org.springframework.util.Assert;

/**
 * Exception thrown to pause agent execution for human-in-the-loop interaction.
 *
 * <p>
 * When thrown during agent execution, this exception signals that the agent should pause
 * and wait for external input. The associated {@link Interrupt} carries the details of
 * what input is needed.
 *
 * @author Spring AI
 * @since 2.0.0
 * @see Interrupt
 * @see InterruptResolver
 */
public class InterruptException extends RuntimeException {

	private final Interrupt interrupt;

	/**
	 * Create a new {@code InterruptException}.
	 * @param interrupt the interrupt that caused the pause, must not be {@code null}
	 */
	public InterruptException(Interrupt interrupt) {
		super("Agent execution interrupted: " + interrupt.getName());
		Assert.notNull(interrupt, "interrupt must not be null");
		this.interrupt = interrupt;
	}

	/**
	 * Return the interrupt that caused the agent to pause.
	 * @return the interrupt, never {@code null}
	 */
	public Interrupt getInterrupt() {
		return this.interrupt;
	}

}
