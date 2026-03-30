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

package org.springframework.ai.strands.agent.loop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.strands.agent.AgentOptions;
import org.springframework.ai.strands.agent.AgentState;
import org.springframework.ai.strands.hooks.HookRegistry;
import org.springframework.ai.strands.hooks.events.AfterModelCallEvent;
import org.springframework.ai.strands.hooks.events.AfterToolCallEvent;
import org.springframework.ai.strands.hooks.events.BeforeModelCallEvent;
import org.springframework.ai.strands.hooks.events.BeforeToolCallEvent;
import org.springframework.ai.strands.hooks.events.MessageAddedEvent;
import org.springframework.ai.tool.ToolCallback;

/**
 * Core agent event loop that iteratively calls a model and executes tools.
 *
 * <p>
 * The event loop performs the following steps on each iteration:
 * <ol>
 * <li>Fires a {@link BeforeModelCallEvent}</li>
 * <li>Calls the {@link ChatModel} with the current messages and tool definitions</li>
 * <li>Fires an {@link AfterModelCallEvent} (with retry support on failure)</li>
 * <li>If the response contains tool-call requests, fires {@link BeforeToolCallEvent} for
 * each tool (with cancel support), executes the tools via {@link ToolCallingManager},
 * fires {@link AfterToolCallEvent} for each, and loops back to step 1</li>
 * <li>If no tool calls are requested, returns the final {@link EventLoopResult}</li>
 * </ol>
 *
 * <p>
 * The loop terminates when the model produces a final answer or the configured maximum
 * number of iterations is exceeded, in which case a
 * {@link MaxIterationsExceededException} is thrown.
 *
 * @author Spring AI
 * @since 2.0.0
 * @see EventLoopResult
 * @see MaxIterationsExceededException
 */
public class AgentEventLoop {

	private static final Logger logger = LoggerFactory.getLogger(AgentEventLoop.class);

	/**
	 * Execute the agent event loop.
	 * @param model the chat model to call
	 * @param state the mutable agent state holding conversation messages
	 * @param options the agent configuration options
	 * @param tools the available tool callbacks
	 * @param toolCallingManager the manager for executing tool calls
	 * @param hookRegistry the hook registry for lifecycle events
	 * @param agentId the identifier of the owning agent
	 * @param invocationState the invocation state map passed through events
	 * @return the final event loop result
	 * @throws MaxIterationsExceededException if the maximum number of iterations is
	 * exceeded
	 */
	@SuppressWarnings("NullAway")
	public EventLoopResult execute(ChatModel model, AgentState state, AgentOptions options, List<ToolCallback> tools,
			ToolCallingManager toolCallingManager, HookRegistry hookRegistry, String agentId,
			Map<String, Object> invocationState) {

		int iterations = 0;
		List<EventLoopResult.ToolCallResult> allToolResults = new ArrayList<>();

		while (iterations < options.getMaxIterations()) {
			iterations++;
			logger.debug("Agent {} iteration {}/{}", agentId, iterations, options.getMaxIterations());

			hookRegistry.invokeCallbacks(new BeforeModelCallEvent(agentId, invocationState));

			ToolCallingChatOptions chatOptions = DefaultToolCallingChatOptions.builder()
				.toolCallbacks(tools)
				.internalToolExecutionEnabled(false)
				.build();

			Prompt prompt = new Prompt(state.getMessages(), chatOptions);

			ChatResponse response;
			try {
				response = model.call(prompt);
			}
			catch (Exception ex) {
				logger.warn("Agent {} model call failed on iteration {}", agentId, iterations, ex);
				AfterModelCallEvent afterEvent = hookRegistry
					.invokeCallbacks(new AfterModelCallEvent(agentId, invocationState, "", null, ex));
				if (afterEvent.isRetry()) {
					logger.debug("Agent {} retrying model call after failure", agentId);
					continue;
				}
				throw ex;
			}

			Generation generation = response.getResult();
			AssistantMessage assistantMessage = generation.getOutput();
			String stopReason = (generation.getMetadata() != null && generation.getMetadata().getFinishReason() != null)
					? generation.getMetadata().getFinishReason() : "end_turn";

			AfterModelCallEvent afterModelEvent = hookRegistry
				.invokeCallbacks(new AfterModelCallEvent(agentId, invocationState, stopReason, assistantMessage, null));
			if (afterModelEvent.isRetry()) {
				logger.debug("Agent {} retrying model call per hook request", agentId);
				continue;
			}

			hookRegistry.invokeCallbacks(new MessageAddedEvent(agentId, assistantMessage));

			if (response.hasToolCalls()) {
				logger.debug("Agent {} processing {} tool calls", agentId, assistantMessage.getToolCalls().size());

				boolean cancelled = false;
				for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
					BeforeToolCallEvent beforeToolEvent = hookRegistry
						.invokeCallbacks(new BeforeToolCallEvent(agentId, toolCall.name(),
								Map.of("arguments", toolCall.arguments() != null ? toolCall.arguments() : "")));
					if (beforeToolEvent.isCancelTool()) {
						logger.debug("Agent {} tool call {} cancelled by hook", agentId, toolCall.name());
						cancelled = true;
						break;
					}
				}

				if (cancelled) {
					return new EventLoopResult(assistantMessage, stopReason, false, List.copyOf(allToolResults));
				}

				ToolExecutionResult toolResult = toolCallingManager.executeToolCalls(prompt, response);

				state.setMessages(new ArrayList<>(toolResult.conversationHistory()));

				for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
					allToolResults.add(new EventLoopResult.ToolCallResult(toolCall.name(), "executed"));
					hookRegistry.invokeCallbacks(new AfterToolCallEvent(agentId, toolCall.name(), toolResult, null));
				}

				if (toolResult.returnDirect()) {
					logger.debug("Agent {} tool returned directly", agentId);
					return new EventLoopResult(assistantMessage, "tool_result", false, List.copyOf(allToolResults));
				}

				continue;
			}

			logger.debug("Agent {} completed with stop reason: {}", agentId, stopReason);
			return new EventLoopResult(assistantMessage, stopReason != null ? stopReason : "end_turn", false,
					List.copyOf(allToolResults));
		}

		throw new MaxIterationsExceededException(options.getMaxIterations());
	}

}
