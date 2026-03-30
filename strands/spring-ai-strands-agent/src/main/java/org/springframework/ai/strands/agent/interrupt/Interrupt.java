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

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Represents an interrupt in the agent execution for human-in-the-loop scenarios.
 *
 * <p>
 * An interrupt pauses the agent and provides data that a human (or external system) must
 * review before the agent can continue. The interrupt is identified by a name and carries
 * arbitrary data.
 *
 * @author Spring AI
 * @since 2.0.0
 * @see InterruptException
 * @see InterruptResolver
 */
public class Interrupt {

	private final String name;

	private final @Nullable Object data;

	/**
	 * Create a new {@code Interrupt}.
	 * @param name the interrupt name, must not be {@code null}
	 * @param data the data associated with the interrupt, may be {@code null}
	 */
	public Interrupt(String name, @Nullable Object data) {
		Assert.notNull(name, "name must not be null");
		this.name = name;
		this.data = data;
	}

	/**
	 * Create a new {@code Interrupt} with no data.
	 * @param name the interrupt name, must not be {@code null}
	 */
	public Interrupt(String name) {
		this(name, null);
	}

	/**
	 * Return the name of the interrupt.
	 * @return the interrupt name, never {@code null}
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the data associated with the interrupt.
	 * @return the interrupt data, may be {@code null}
	 */
	public @Nullable Object getData() {
		return this.data;
	}

	@Override
	public String toString() {
		return "Interrupt{name='" + this.name + "', data=" + this.data + "}";
	}

}
