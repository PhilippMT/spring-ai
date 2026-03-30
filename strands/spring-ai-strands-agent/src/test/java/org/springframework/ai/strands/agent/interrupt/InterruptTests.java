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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link Interrupt} and {@link InterruptException}.
 *
 * @author Spring AI
 */
class InterruptTests {

	@Test
	void testInterruptCreation() {
		Interrupt interrupt = new Interrupt("approval", "Please approve this action.");

		assertThat(interrupt.getName()).isEqualTo("approval");
		assertThat(interrupt.getData()).isEqualTo("Please approve this action.");
	}

	@Test
	void testInterruptCreationWithoutData() {
		Interrupt interrupt = new Interrupt("pause");

		assertThat(interrupt.getName()).isEqualTo("pause");
		assertThat(interrupt.getData()).isNull();
	}

	@Test
	void testInterruptToString() {
		Interrupt interrupt = new Interrupt("test", "data");
		assertThat(interrupt.toString()).contains("test").contains("data");
	}

	@Test
	void testInterruptExceptionThrownAndCaught() {
		Interrupt interrupt = new Interrupt("confirm", "Are you sure?");

		assertThatThrownBy(() -> {
			throw new InterruptException(interrupt);
		}).isInstanceOf(InterruptException.class)
			.hasMessageContaining("confirm")
			.satisfies(ex -> assertThat(((InterruptException) ex).getInterrupt()).isSameAs(interrupt));
	}

	@Test
	void testInterruptExceptionContainsInterrupt() {
		Interrupt interrupt = new Interrupt("review", 42);
		InterruptException exception = new InterruptException(interrupt);

		assertThat(exception.getInterrupt()).isNotNull();
		assertThat(exception.getInterrupt().getName()).isEqualTo("review");
		assertThat(exception.getInterrupt().getData()).isEqualTo(42);
	}

}
