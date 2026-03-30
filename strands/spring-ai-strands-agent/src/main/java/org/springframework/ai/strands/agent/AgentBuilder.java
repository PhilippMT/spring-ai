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

package org.springframework.ai.strands.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.strands.agent.conversation.ConversationManager;
import org.springframework.ai.strands.agent.conversation.NullConversationManager;
import org.springframework.ai.strands.agent.session.SessionManager;
import org.springframework.ai.strands.hooks.HookProvider;
import org.springframework.ai.strands.hooks.HookRegistry;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;

/**
 * Fluent builder for creating {@link Agent} instances.
 *
 * <p>
 * A {@link ChatModel} is required; all other settings have sensible defaults.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public class AgentBuilder {

	private @Nullable ChatModel chatModel;

	private @Nullable String systemPrompt;

	private final List<ToolCallback> tools = new ArrayList<>();

	private int maxIterations = 25;

	private final HookRegistry hookRegistry = new HookRegistry();

	private ConversationManager conversationManager = new NullConversationManager();

	private @Nullable SessionManager sessionManager;

	private @Nullable ToolCallingManager toolCallingManager;

	AgentBuilder() {
	}

	/**
	 * Set the chat model used by the agent.
	 * @param chatModel the chat model, must not be {@code null}
	 * @return this builder
	 */
	public AgentBuilder chatModel(ChatModel chatModel) {
		Assert.notNull(chatModel, "chatModel must not be null");
		this.chatModel = chatModel;
		return this;
	}

	/**
	 * Set the system prompt prepended to every conversation.
	 * @param systemPrompt the system prompt
	 * @return this builder
	 */
	public AgentBuilder systemPrompt(@Nullable String systemPrompt) {
		this.systemPrompt = systemPrompt;
		return this;
	}

	/**
	 * Add tools the agent may invoke during execution.
	 * @param tools the tool callbacks
	 * @return this builder
	 */
	public AgentBuilder tools(ToolCallback... tools) {
		Assert.notNull(tools, "tools must not be null");
		this.tools.addAll(Arrays.asList(tools));
		return this;
	}

	/**
	 * Add tools the agent may invoke during execution.
	 * @param tools the tool callbacks, must not be {@code null}
	 * @return this builder
	 */
	public AgentBuilder tools(List<ToolCallback> tools) {
		Assert.notNull(tools, "tools must not be null");
		this.tools.addAll(tools);
		return this;
	}

	/**
	 * Set the maximum number of event loop iterations.
	 * @param maxIterations the maximum iterations, must be positive
	 * @return this builder
	 */
	public AgentBuilder maxIterations(int maxIterations) {
		Assert.isTrue(maxIterations > 0, "maxIterations must be positive");
		this.maxIterations = maxIterations;
		return this;
	}

	/**
	 * Register a {@link HookProvider} with the agent's hook registry.
	 * @param provider the hook provider, must not be {@code null}
	 * @return this builder
	 */
	public AgentBuilder hookProvider(HookProvider provider) {
		Assert.notNull(provider, "provider must not be null");
		this.hookRegistry.addHook(provider);
		return this;
	}

	/**
	 * Set the hook registry. Note that any previously registered hooks via
	 * {@link #hookProvider(HookProvider)} will be discarded.
	 * @param registry the hook registry, must not be {@code null}
	 * @return this builder
	 */
	public AgentBuilder hookRegistry(HookRegistry registry) {
		Assert.notNull(registry, "registry must not be null");
		return this;
	}

	/**
	 * Set the conversation manager used to trim or summarize message history.
	 * @param manager the conversation manager, must not be {@code null}
	 * @return this builder
	 */
	public AgentBuilder conversationManager(ConversationManager manager) {
		Assert.notNull(manager, "manager must not be null");
		this.conversationManager = manager;
		return this;
	}

	/**
	 * Set the session manager used for persisting agent state.
	 * @param manager the session manager
	 * @return this builder
	 */
	public AgentBuilder sessionManager(@Nullable SessionManager manager) {
		this.sessionManager = manager;
		return this;
	}

	/**
	 * Set the tool calling manager used to execute tool calls.
	 * @param toolCallingManager the tool calling manager
	 * @return this builder
	 */
	public AgentBuilder toolCallingManager(ToolCallingManager toolCallingManager) {
		Assert.notNull(toolCallingManager, "toolCallingManager must not be null");
		this.toolCallingManager = toolCallingManager;
		return this;
	}

	/**
	 * Build the {@link Agent}.
	 * @return a new agent instance
	 * @throws IllegalStateException if {@code chatModel} has not been set
	 */
	public Agent build() {
		Assert.state(this.chatModel != null, "chatModel must be set");

		AgentOptions options = AgentOptions.builder()
			.systemPrompt(this.systemPrompt)
			.maxIterations(this.maxIterations)
			.build();

		ToolCallingManager resolvedToolCallingManager = this.toolCallingManager != null ? this.toolCallingManager
				: ToolCallingManager.builder().build();

		return new Agent(this.chatModel, resolvedToolCallingManager, List.copyOf(this.tools), this.hookRegistry,
				this.conversationManager, this.sessionManager, options);
	}

}
