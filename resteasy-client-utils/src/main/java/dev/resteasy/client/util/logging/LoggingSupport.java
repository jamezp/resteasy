/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.client.util.logging;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jboss.logging.Messages;

/**
 * A support utility for creating message bundle types. This supports using JBoss Logging 3.5.x and 3.6.x.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@SuppressWarnings("removal")
class LoggingSupport {
    private static final MethodHandle getBundle;

    static {
        MethodHandle messageBundle = null;
        try {
            messageBundle = MethodHandles.publicLookup()
                    .findStatic(Messages.class, "getBundle",
                            MethodType.methodType(Object.class, MethodHandles.Lookup.class, Class.class));
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
        }
        getBundle = messageBundle;
    }

    /**
     * Creates a message bundle instance for the specified bundle interface.
     * <p>
     * If JBoss Logging 3.6.x or later is available, this method uses the
     * {@link Messages#getBundle(MethodHandles.Lookup, Class)} method. Otherwise, it falls back to the deprecated
     * {@link Messages#getBundle(Class)} method for compatibility with JBoss Logging 3.5.x.
     * </p>
     *
     * @param <T>         the bundle interface type
     * @param bundleClass the bundle interface class
     *
     * @return a message bundle instance
     */
    static <T> T getBundle(final Class<T> bundleClass) {
        try {
            if (getBundle != null) {
                return bundleClass.cast(getBundle.invokeWithArguments(MethodHandles.lookup(), bundleClass));
            }
            // Use the old method
            return Messages.getBundle(bundleClass);
        } catch (Throwable t) {
            // Fallback
            return Messages.getBundle(bundleClass);
        }
    }
}
