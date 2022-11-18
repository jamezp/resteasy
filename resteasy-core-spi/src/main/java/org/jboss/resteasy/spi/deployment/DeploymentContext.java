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

package org.jboss.resteasy.spi.deployment;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

/**
 * A contex
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public final class DeploymentContext implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(DeploymentContext.class);
    // TODO (jrp) this should probably be the GlobalDeploymentContextSelector, however for testing we'll leave it now.
    // TODO (jrp) for WildFly, or really any multi-tenant container, we do need this ContextClassLoaderDeploymentContextSelector.
    // TODO (jrp) deployments MUST be stopped though
    private static final DeploymentContextSelector DEFAULT_SELECTOR = new ContextClassLoaderDeploymentContextSelector(GlobalDeploymentContextSelector.INSTANCE);

    private static volatile ThreadLocalDeploymentContextSelector CONTEXT_SELECTOR = new ThreadLocalDeploymentContextSelector(DEFAULT_SELECTOR);

    private final String name;
    private final Map<Class<?>, Object> context;

    public DeploymentContext() {
        context = new ConcurrentHashMap<>();
        this.name = "unnamed";
    }

    public DeploymentContext(final String name) {
        context = new ConcurrentHashMap<>();
        this.name = name;
    }

    public static void setContextSelector(final DeploymentContextSelector contextSelector) {
        if (contextSelector instanceof ThreadLocalDeploymentContextSelector) {
            CONTEXT_SELECTOR = (ThreadLocalDeploymentContextSelector) contextSelector;
        } else {
            // TODO (jrp) should we do a permissions check? We probably should
            CONTEXT_SELECTOR = new ThreadLocalDeploymentContextSelector(Objects.requireNonNull(contextSelector)); // TODO (jrp) i18n
        }
    }

    public static DeploymentContextSelector getDeploymentContextSelector() {
        return CONTEXT_SELECTOR.defaultSelector;
    }

    public static DeploymentContext getDeploymentContext() {
        return CONTEXT_SELECTOR.get();
    }

    // TODO (jrp) do we need this or should the DeploymentThreadContext handle it?
    public static void pushDeploymentContext(final DeploymentContext context) {
        if (context != null) {
            // TODO (jrp) change this to debug
            LOGGER.warnf("dc-push: Pushed context %s onto thread %s", context, Thread.currentThread());
            CONTEXT_SELECTOR.localContext.set(context);
        } else {
            LOGGER.warnf("dc-push: Attempting to push null onto thread %s", Thread.currentThread());
        }
    }

    public static void popDeploymentContext() {
        LOGGER.warnf("dc-pop: Removed context from thread %s", Thread.currentThread());
        CONTEXT_SELECTOR.localContext.remove();
    }

    public <T> T get(final Class<T> type) {
        return type.cast(context.get(type));
    }

    public <T> T computeIfAbsent(final Class<T> type, final Supplier<T> dft) {
        return type.cast(context.computeIfAbsent(type, (value) -> dft.get()));
    }

    public <T> T put(final Class<T> type, final T value) {
        return type.cast(context.put(type, value));
    }

    // TODO (jrp) maybe add a putAll

    @Override
    public void close() {
        CONTEXT_SELECTOR.localContext.remove();
        context.clear();
    }

    @Override
    public String toString() {
        //return "DeploymentContext[name=" + name + "]";
        // TODO (jrp) remove this one
        return "DeploymentContext[name=" + name + ", context=" + context + "]";
    }

    private static class ThreadLocalDeploymentContextSelector implements DeploymentContextSelector {

        private final ThreadLocal<DeploymentContext> localContext = new ThreadLocal<>();
        private final DeploymentContextSelector defaultSelector;

        private ThreadLocalDeploymentContextSelector(final DeploymentContextSelector defaultSelector) {
            this.defaultSelector = defaultSelector;
        }

        @Override
        public DeploymentContext get() {
            final DeploymentContext local = localContext.get();
            return local != null ? local : defaultSelector.get();
        }
    }
}
