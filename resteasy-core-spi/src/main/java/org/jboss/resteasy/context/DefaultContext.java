/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.resteasy.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class DefaultContext implements Context {

    private final String name;
    private final Map<Class<?>, Object> context;

    protected DefaultContext() {
        context = new ConcurrentHashMap<>();
        this.name = SecurityActions.getClassLoader().getName();
    }

    protected DefaultContext(final DefaultContext toCopy) {
        context = new ConcurrentHashMap<>(toCopy.context);
        this.name = toCopy.name;
    }

    @Override
    public <T> T get(final Class<T> type) {
        return type.cast(context.get(type));
    }

    @Override
    public <T> T computeIfAbsent(final Class<T> type, final Supplier<T> dft) {
        return type.cast(context.computeIfAbsent(type, (value) -> dft.get()));
    }

    @Override
    public <T> T put(final Class<T> type, final T value) {
        return type.cast(context.put(type, value));
    }

    @Override
    public void close() {
        context.clear();
    }

    @Override
    public String toString() {
        //return "DeploymentContext[name=" + name + "]";
        // TODO (jrp) remove this one
        return getClass().getSimpleName() + "[name=" + name + ", context=" + context + "]";
    }
}
