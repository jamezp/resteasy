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

import java.util.function.Supplier;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
// TODO (jrp) should this be Contextual or is Context okay?
public interface Context extends AutoCloseable {

    <T> T get(Class<T> type);

    <T> T computeIfAbsent(Class<T> type, Supplier<T> dft);

    <T> T put(Class<T> type, T value);

    @Override
    void close();

    static Context current() {
        final Context current = ContextManager.getInstance().get();
        if (current == null) {
            // TODO (jrp) i18n
            throw new RuntimeException("No context found");
        }
        return current;
    }

    static boolean register(final Context context) {
        return ContextManager.getInstance().register(context);
    }

    static boolean unregister(final Context context) {
        return ContextManager.getInstance().unregister(context);
    }

    static Context push(final Context context) {
        return ContextManager.getInstance().push(context);
    }

    static Context pop() {
        return ContextManager.getInstance().pop();
    }

    static Context createAndRegister() {
        return ContextManager.getInstance().createAndRegister();
    }

    static Context create() {
        return ContextManager.getInstance().create();
    }
}
