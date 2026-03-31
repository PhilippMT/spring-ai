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

package org.springframework.ai.strands.telemetry;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsCollector}.
 *
 * @author Spring AI
 * @since 2.0.0
 */
class MetricsCollectorTests {

	private MetricsCollector metricsCollector;

	@BeforeEach
	void setUp() {
		TestObservationRegistry observationRegistry = TestObservationRegistry.create();
		this.metricsCollector = new MetricsCollector(observationRegistry);
	}

	@Test
	void recordInvocationIncrementsCount() {
		assertThat(this.metricsCollector.getInvocationCount()).isZero();

		this.metricsCollector.recordInvocation("agent-1", 100);
		assertThat(this.metricsCollector.getInvocationCount()).isEqualTo(1);

		this.metricsCollector.recordInvocation("agent-1", 200);
		assertThat(this.metricsCollector.getInvocationCount()).isEqualTo(2);

		this.metricsCollector.recordInvocation("agent-2", 150);
		assertThat(this.metricsCollector.getInvocationCount()).isEqualTo(3);
	}

	@Test
	void recordToolCallTracksSuccessAndErrors() {
		assertThat(this.metricsCollector.getToolCallCount()).isZero();
		assertThat(this.metricsCollector.getErrorCount()).isZero();

		this.metricsCollector.recordToolCall("search", 50, true);
		assertThat(this.metricsCollector.getToolCallCount()).isEqualTo(1);
		assertThat(this.metricsCollector.getErrorCount()).isZero();

		this.metricsCollector.recordToolCall("calculator", 30, false);
		assertThat(this.metricsCollector.getToolCallCount()).isEqualTo(2);
		assertThat(this.metricsCollector.getErrorCount()).isEqualTo(1);

		this.metricsCollector.recordToolCall("fetch", 100, true);
		assertThat(this.metricsCollector.getToolCallCount()).isEqualTo(3);
		assertThat(this.metricsCollector.getErrorCount()).isEqualTo(1);
	}

	@Test
	void recordTokenUsageAccumulatesTokens() {
		assertThat(this.metricsCollector.getTotalInputTokens()).isZero();
		assertThat(this.metricsCollector.getTotalOutputTokens()).isZero();

		this.metricsCollector.recordTokenUsage("agent-1", 100, 50);
		assertThat(this.metricsCollector.getTotalInputTokens()).isEqualTo(100);
		assertThat(this.metricsCollector.getTotalOutputTokens()).isEqualTo(50);

		this.metricsCollector.recordTokenUsage("agent-1", 200, 75);
		assertThat(this.metricsCollector.getTotalInputTokens()).isEqualTo(300);
		assertThat(this.metricsCollector.getTotalOutputTokens()).isEqualTo(125);

		this.metricsCollector.recordTokenUsage("agent-2", 150, 100);
		assertThat(this.metricsCollector.getTotalInputTokens()).isEqualTo(450);
		assertThat(this.metricsCollector.getTotalOutputTokens()).isEqualTo(225);
	}

}
