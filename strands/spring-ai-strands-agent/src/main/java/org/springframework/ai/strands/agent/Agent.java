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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.strands.agent.conversation.ConversationManager;
import org.springframework.ai.strands.agent.loop.AgentEventLoop;
import org.springframework.ai.strands.agent.loop.EventLoopResult;
import org.springframework.ai.strands.agent.session.SessionManager;
import org.springframework.ai.strands.hooks.HookRegistry;
import org.springframework.ai.strands.hooks.events.AfterInvocationEvent;
import org.springframework.ai.strands.hooks.events.AgentInitializedEvent;
import org.springframework.ai.strands.hooks.events.BeforeInvocationEvent;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;

/**
 * Autonomous AI agent that iteratively calls a model and executes tools until a final
 * answer is produced.
 *
 * <p>
 * The agent implements the core Strands agent loop: it sends a prompt to the
 * {@link ChatModel}, inspects the response for tool-call requests, executes them via the
 * {@link ToolCallingManager}, and feeds the results back to the model. This cycle repeats
 * until the model produces a final answer or the configured maximum number of iterations
 * is reached.
 *
 * <p>
 * Lifecycle hook events are fired at every stage through the {@link HookRegistry}.
 *
 * <p>
 * Use {@link #builder()} to create instances.
 *
 * @author Spring AI
 * @since 2.0.0
 * @see AgentBuilder
 * @see AgentEventLoop
 */
public class Agent {

	private static final Logger logger = LoggerFactory.getLogger(Agent.class);

	private final String agentId;

	private final ChatModel chatModel;

	private final ToolCallingManager toolCallingManager;

	private final List<ToolCallback> tools;

	private final HookRegistry hookRegistry;

	private final ConversationManager conversationManager;

	private final @Nullable SessionManager sessionManager;

	private final AgentOptions options;

	private final AgentEventLoop eventLoop;

	Agent(ChatModel chatModel, ToolCallingManager toolCallingManager, List<ToolCallback> tools,
			HookRegistry hookRegistry, ConversationManager conversationManager, @Nullable SessionManager sessionManager,
			AgentOptions options) {
		Assert.notNull(chatModel, "chatModel must not be null");
		Assert.notNull(toolCallingManager, "toolCallingManager must not be null");
		Assert.notNull(tools, "tools must not be null");
		Assert.notNull(hookRegistry, "hookRegistry must not be null");
		Assert.notNull(conversationManager, "conversationManager must not be null");
		Assert.notNull(options, "options must not be null");
		this.agentId = UUID.randomUUID().toString();
		this.chatModel = chatModel;
		this.toolCallingManager = toolCallingManager;
		this.tools = List.copyOf(tools);
		this.hookRegistry = hookRegistry;
		this.conversationManager = conversationManager;
		this.sessionManager = sessionManager;
		this.options = options;
		this.eventLoop = new AgentEventLoop();

		this.hookRegistry.invokeCallbacks(new AgentInitializedEvent(this.agentId));
		logger.debug("Agent {} initialized with {} tools", this.agentId, this.tools.size());
	}

	/**
	 * Invoke the agent with the given user message.
	 * @param userMessage the user's natural-language input, must not be {@code null}
	 * @return the agent result containing the final assistant response
	 */
	public AgentResult call(String userMessage) {
		return call(userMessage, new HashMap<>());
	}

	/**
	 * Invoke the agent with the given user message and additional invocation state.
	 * @param userMessage the user's natural-language input, must not be {@code null}
	 * @param invocationState extra state passed through the agent lifecycle, must not be
	 * {@code null}
	 * @return the agent result containing the final assistant response
	 */
	public AgentResult call(String userMessage, Map<String, Object> invocationState) {
		Assert.notNull(userMessage, "userMessage must not be null");
		Assert.notNull(invocationState, "invocationState must not be null");

		AgentState state = new AgentState();

		if (this.options.getSystemPrompt() != null) {
			state.addMessage(new SystemMessage(this.options.getSystemPrompt()));
		}
		state.addMessage(new UserMessage(userMessage));

		Map<String, Object> mergedState = new HashMap<>(this.options.getInvocationState());
		mergedState.putAll(invocationState);

		BeforeInvocationEvent beforeEvent = this.hookRegistry
			.invokeCallbacks(new BeforeInvocationEvent(this.agentId, mergedState, state.getMessages()));
		state.setMessages(new ArrayList<>(beforeEvent.getMessages()));

		this.conversationManager.apply(state);

		EventLoopResult result = this.eventLoop.execute(this.chatModel, state, this.options, this.tools,
				this.toolCallingManager, this.hookRegistry, this.agentId, mergedState);

		AgentResult agentResult = new AgentResult(result.getMessage(), result.getStopReason(), state, new HashMap<>());

		this.hookRegistry.invokeCallbacks(new AfterInvocationEvent(this.agentId, mergedState, agentResult));

		if (this.sessionManager != null) {
			this.sessionManager.save(state.getSessionId(), state);
		}

		return agentResult;
	}

	/**
	 * Create a new {@link AgentBuilder}.
	 * @return a new builder instance
	 */
	public static AgentBuilder builder() {
		return new AgentBuilder();
	}

	/**
	 * Return the unique identifier of this agent.
	 * @return the agent identifier
	 */
	public String getAgentId() {
		return this.agentId;
	}

	/**
	 * Return the hook registry associated with this agent.
	 * @return the hook registry
	 */
	public HookRegistry getHookRegistry() {
		return this.hookRegistry;
	}

}
