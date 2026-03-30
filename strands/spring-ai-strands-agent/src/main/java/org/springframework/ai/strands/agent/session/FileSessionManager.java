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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.strands.agent.AgentState;
import org.springframework.util.Assert;

/**
 * A file-based {@link SessionManager} that persists agent state as JSON files using
 * Jackson.
 *
 * <p>
 * Each session is stored as a JSON file named {@code {sessionId}.json} in the configured
 * directory. The file contains a serialized map of the session's messages and attributes.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class FileSessionManager implements SessionManager {

	private static final String FILE_EXTENSION = ".json";

	private final Path directory;

	private final ObjectMapper objectMapper;

	/**
	 * Create a new {@code FileSessionManager} that stores sessions in the given
	 * directory.
	 * @param directory the storage directory, must not be {@code null}
	 */
	public FileSessionManager(Path directory) {
		this(directory, new ObjectMapper());
	}

	/**
	 * Create a new {@code FileSessionManager} with a custom {@link ObjectMapper}.
	 * @param directory the storage directory, must not be {@code null}
	 * @param objectMapper the Jackson object mapper, must not be {@code null}
	 */
	public FileSessionManager(Path directory, ObjectMapper objectMapper) {
		Assert.notNull(directory, "directory must not be null");
		Assert.notNull(objectMapper, "objectMapper must not be null");
		this.directory = directory;
		this.objectMapper = objectMapper;
		try {
			Files.createDirectories(directory);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to create session directory: " + directory, ex);
		}
	}

	@Override
	public void save(String sessionId, AgentState state) {
		Assert.notNull(sessionId, "sessionId must not be null");
		Assert.notNull(state, "state must not be null");
		Path file = resolveFile(sessionId);
		try {
			Map<String, Object> data = new HashMap<>();
			data.put("sessionId", state.getSessionId());
			data.put("attributes", state.getAttributes());
			this.objectMapper.writeValue(file.toFile(), data);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to save session: " + sessionId, ex);
		}
	}

	@Override
	public Optional<AgentState> load(String sessionId) {
		Assert.notNull(sessionId, "sessionId must not be null");
		Path file = resolveFile(sessionId);
		if (!Files.exists(file)) {
			return Optional.empty();
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> data = this.objectMapper.readValue(file.toFile(), Map.class);
			AgentState state = new AgentState(sessionId);
			if (data.containsKey("attributes") && data.get("attributes") instanceof Map<?, ?> attrs) {
				for (Map.Entry<?, ?> entry : attrs.entrySet()) {
					state.setAttribute(String.valueOf(entry.getKey()), entry.getValue());
				}
			}
			return Optional.of(state);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to load session: " + sessionId, ex);
		}
	}

	@Override
	public void delete(String sessionId) {
		Assert.notNull(sessionId, "sessionId must not be null");
		Path file = resolveFile(sessionId);
		try {
			Files.deleteIfExists(file);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to delete session: " + sessionId, ex);
		}
	}

	@Override
	public List<String> listSessions() {
		try (Stream<Path> files = Files.list(this.directory)) {
			return files.filter(p -> p.toString().endsWith(FILE_EXTENSION))
				.map(p -> p.getFileName().toString().replace(FILE_EXTENSION, ""))
				.toList();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to list sessions", ex);
		}
	}

	private Path resolveFile(String sessionId) {
		return this.directory.resolve(sessionId + FILE_EXTENSION);
	}

}
