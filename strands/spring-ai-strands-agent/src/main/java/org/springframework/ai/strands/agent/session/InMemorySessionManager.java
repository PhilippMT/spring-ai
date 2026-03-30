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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.strands.agent.AgentState;
import org.springframework.util.Assert;

/**
 * An in-memory {@link SessionManager} backed by a {@link ConcurrentHashMap}.
 *
 * <p>
 * Suitable for development and testing. State is lost when the JVM shuts down.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class InMemorySessionManager implements SessionManager {

	private final Map<String, AgentState> sessions = new ConcurrentHashMap<>();

	@Override
	public void save(String sessionId, AgentState state) {
		Assert.notNull(sessionId, "sessionId must not be null");
		Assert.notNull(state, "state must not be null");
		this.sessions.put(sessionId, state);
	}

	@Override
	public Optional<AgentState> load(String sessionId) {
		Assert.notNull(sessionId, "sessionId must not be null");
		return Optional.ofNullable(this.sessions.get(sessionId));
	}

	@Override
	public void delete(String sessionId) {
		Assert.notNull(sessionId, "sessionId must not be null");
		this.sessions.remove(sessionId);
	}

	@Override
	public List<String> listSessions() {
		return new ArrayList<>(this.sessions.keySet());
	}

}
