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

package org.springframework.ai.strands.agent.session;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.strands.agent.AgentState;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InMemorySessionManager}.
 *
 * @author Spring AI
 */
class InMemorySessionManagerTests {

	private InMemorySessionManager sessionManager;

	@BeforeEach
	void setUp() {
		this.sessionManager = new InMemorySessionManager();
	}

	@Test
	void testSaveAndLoad() {
		AgentState state = new AgentState("session-1");
		state.setAttribute("key", "value");

		this.sessionManager.save("session-1", state);

		Optional<AgentState> loaded = this.sessionManager.load("session-1");
		assertThat(loaded).isPresent();
		assertThat(loaded.get().getSessionId()).isEqualTo("session-1");
		assertThat(loaded.get().getAttribute("key")).isEqualTo("value");
	}

	@Test
	void testLoadNonExistent() {
		Optional<AgentState> loaded = this.sessionManager.load("does-not-exist");
		assertThat(loaded).isEmpty();
	}

	@Test
	void testDelete() {
		AgentState state = new AgentState("session-2");
		this.sessionManager.save("session-2", state);
		assertThat(this.sessionManager.load("session-2")).isPresent();

		this.sessionManager.delete("session-2");
		assertThat(this.sessionManager.load("session-2")).isEmpty();
	}

	@Test
	void testListSessions() {
		this.sessionManager.save("s1", new AgentState("s1"));
		this.sessionManager.save("s2", new AgentState("s2"));
		this.sessionManager.save("s3", new AgentState("s3"));

		List<String> sessions = this.sessionManager.listSessions();
		assertThat(sessions).containsExactlyInAnyOrder("s1", "s2", "s3");
	}

	@Test
	void testDeleteNonExistentDoesNotThrow() {
		this.sessionManager.delete("no-such-session");
	}

	@Test
	void testOverwriteSession() {
		AgentState state1 = new AgentState("s1");
		state1.setAttribute("version", "1");
		this.sessionManager.save("s1", state1);

		AgentState state2 = new AgentState("s1");
		state2.setAttribute("version", "2");
		this.sessionManager.save("s1", state2);

		Optional<AgentState> loaded = this.sessionManager.load("s1");
		assertThat(loaded).isPresent();
		assertThat(loaded.get().getAttribute("version")).isEqualTo("2");
	}

}
