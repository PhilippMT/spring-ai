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

import org.springframework.ai.strands.agent.AgentState;

/**
 * Strategy interface for persisting and retrieving {@link AgentState} across invocations.
 *
 * @author Spring AI
 * @since 2.0.0
 * @see InMemorySessionManager
 * @see FileSessionManager
 */
public interface SessionManager {

	/**
	 * Persist the agent state for the given session identifier.
	 * @param sessionId the session identifier, must not be {@code null}
	 * @param state the agent state to save, must not be {@code null}
	 */
	void save(String sessionId, AgentState state);

	/**
	 * Load a previously persisted agent state.
	 * @param sessionId the session identifier, must not be {@code null}
	 * @return an {@link Optional} containing the state if found, or empty
	 */
	Optional<AgentState> load(String sessionId);

	/**
	 * Delete a persisted session.
	 * @param sessionId the session identifier, must not be {@code null}
	 */
	void delete(String sessionId);

	/**
	 * List all known session identifiers.
	 * @return a list of session identifiers
	 */
	List<String> listSessions();

}
