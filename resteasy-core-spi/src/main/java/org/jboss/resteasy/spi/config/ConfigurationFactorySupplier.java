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

package org.jboss.resteasy.spi.config;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class ConfigurationFactorySupplier implements Supplier<ConfigurationFactory> {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationFactorySupplier.class);
    private static final Map<ClassLoader, Holder> TO_DELETE = new ConcurrentHashMap<>();

    static final ConfigurationFactorySupplier INSTANCE = new ConfigurationFactorySupplier();

    private ConfigurationFactorySupplier() {
    }

    @Override
    public ConfigurationFactory get() {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final Holder holder = TO_DELETE.compute(classLoader, (cl, current) -> {
            if (current == null) {
                LOGGER.errorf("Adding new Holder");
                return new Holder();
            }
            LOGGER.errorf("Adding TO Holder %s", current);
            return current.add();
        });
        final int count = holder.counter.get();
        if (count > 1) {
            final Throwable t = new Throwable("OldStackTrace");
            t.setStackTrace(holder.stack);
            final RuntimeException e = new RuntimeException("StackTrace");
            e.addSuppressed(t);
            LOGGER.warnf(e, "ConfigurationFactory#get - %s - %d", classLoader, count);
        }
        if (System.getSecurityManager() == null) {
            final ServiceLoader<ConfigurationFactory> loader = ServiceLoader.load(ConfigurationFactory.class);
            ConfigurationFactory current = null;
            for (ConfigurationFactory factory : loader) {
                if (current == null) {
                    current = factory;
                } else if (factory.priority() < current.priority()) {
                    current = factory;
                }
            }
            return current == null ? () -> Integer.MAX_VALUE : current;
        }
        return AccessController.doPrivileged((PrivilegedAction<ConfigurationFactory>) () -> {
            final ServiceLoader<ConfigurationFactory> loader = ServiceLoader.load(ConfigurationFactory.class);
            ConfigurationFactory current = null;
            for (ConfigurationFactory factory : loader) {
                if (current == null) {
                    current = factory;
                } else if (factory.priority() < current.priority()) {
                    current = factory;
                }
            }
            return current == null ? () -> Integer.MAX_VALUE : current;
        });
    }

    private static class Holder {
        final StackTraceElement[] stack;
        final AtomicInteger counter = new AtomicInteger(1);

        private Holder() {
            stack = new RuntimeException().getStackTrace();
        }

        Holder add() {
            LOGGER.warnf("New count: %d", counter.incrementAndGet());
            return this;
        }
    }
}
