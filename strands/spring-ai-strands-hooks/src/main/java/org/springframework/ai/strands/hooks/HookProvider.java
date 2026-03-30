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

package org.springframework.ai.strands.hooks;

/**
 * Interface for components that provide hook callbacks.
 *
 * <p>
 * Implementations register one or more callbacks with the supplied {@link HookRegistry}
 * during {@link #registerHooks(HookRegistry)}. This allows hook providers to be
 * discovered and configured independently of the registry itself.
 *
 * @author Spring AI
 * @since 2.0.0
 */
public interface HookProvider {

	/**
	 * Register hook callbacks with the given registry.
	 * @param registry the hook registry to register callbacks with, never {@code null}
	 */
	void registerHooks(HookRegistry registry);

}
