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

import static java.lang.System.getSecurityManager;

import java.security.Permission;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class DefaultContextManager extends ContextManager {

    private static final Permission REGISTER_CONTEXT_PERMISSION = new RuntimePermission("registerContext", null);
    private static final Permission UNREGISTER_CONTEXT_PERMISSION = new RuntimePermission("unregisterContext", null);

    private final Map<ClassLoader, Context> contextMap = new ConcurrentHashMap<>();
    private final ThreadLocal<Context> localContext = new ThreadLocal<>();

    @Override
    public Context get() {
        Context context = localContext.get();
        if (context == null) {
            context = contextMap.get(SecurityActions.getClassLoader());
        }
        if (context == null) {
            // TODO (jrp) i18n
            throw new RuntimeException("No context found");
        }
        return context;
    }

    @Override
    public boolean register(final Context context) {
        final SecurityManager sm = getSecurityManager();
        if (sm != null) {
            sm.checkPermission(REGISTER_CONTEXT_PERMISSION);
        }
        if (contextMap.putIfAbsent(SecurityActions.getClassLoader(), context) != null) {
            // TODO (jrp) what we really need is to check the current context, if it's the same as the context we're setting it should be safe to ignore.
            //throw new IllegalArgumentException("ClassLoader instance is already registered to a deployment context (" + classLoader + ")");
            //return false;
        }
        return true;
    }

    @Override
    public boolean unregister(final Context context) {
        final SecurityManager sm = getSecurityManager();
        if (sm != null) {
            sm.checkPermission(UNREGISTER_CONTEXT_PERMISSION);
        }
        // TODO (jrp) should we care about the context here?
        return contextMap.remove(SecurityActions.getClassLoader(), context);
    }

    @Override
    public Context push(final Context context) {
        final Context current = localContext.get();
        if (current != null && !Objects.requireNonNull(context).equals(current)) {
            throw new RuntimeException(String.format("Leaked context found. Current %s attempting to set %s", current, context));
        }
        localContext.set(context);
        return current;
    }

    @Override
    public Context pop() {
        final Context current = localContext.get();
        localContext.remove();
        return current;
    }
}
