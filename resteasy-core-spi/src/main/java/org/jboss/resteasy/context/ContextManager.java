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

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.resteasy.spi.PriorityServiceLoader;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
// TODO (jrp) the entry points for the rest request should have a callback of some sort to set the current scope
// TODO (jrp) do we really need a ContextManager? Context just have a push()/pop() and maybe a pushLocal()/popLocal()
// TODO (jrp) or something similar?
public abstract class ContextManager {

    protected static ContextManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    public abstract Context get();

    public abstract boolean register(Context context);

    public abstract boolean unregister(Context context);

    public Context push() {
        return push(get());
    }

    public abstract Context push(Context context);

    public abstract Context pop();

    public Context create() {
        // TODO (jrp) require permissions for this
        return new DefaultContext();
    }

    public Context createAndRegister() {
        // TODO (jrp) require permissions for this
        final Context context = create();
        if (!register(context)) {
            // TODO (jrp) i18n
            throw new RuntimeException("Failed to register context " + context);
        }
        return context;
    }

    private static class LazyHolder {
        private static final ContextManager INSTANCE;

        static {
            final PrivilegedAction<ContextManager> lookupAction = () -> {
                final PriorityServiceLoader<ContextManager> loader = PriorityServiceLoader.load(ContextManager.class);
                return loader.first().orElseGet(DefaultContextManager::new);
            };
            if (System.getSecurityManager() == null) {
                INSTANCE = lookupAction.run();
            } else {
                INSTANCE = AccessController.doPrivileged(lookupAction);
            }
        }
    }
}
