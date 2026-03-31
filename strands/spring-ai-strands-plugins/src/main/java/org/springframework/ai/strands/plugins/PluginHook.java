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

package org.springframework.ai.strands.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.ai.strands.hooks.BaseHookEvent;

/**
 * Marks a method in a {@link Plugin} subclass as a hook callback. The annotated method
 * will be auto-discovered during plugin construction and registered with the
 * {@link org.springframework.ai.strands.hooks.HookRegistry}.
 *
 * <p>
 * The event type can be specified explicitly via {@link #value()}, or inferred from the
 * method's first parameter type. The method must accept exactly one parameter that
 * extends {@link BaseHookEvent}.
 *
 * @author Spring AI
 * @since 2.0.0
 * @see Plugin
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginHook {

	/**
	 * The event types this hook callback should be registered for. When empty, the event
	 * type is inferred from the method's first parameter type.
	 * @return the event types to listen for
	 */
	Class<? extends BaseHookEvent>[] value() default {};

}
