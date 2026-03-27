/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.resteasy_jaxrs.i18n;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.jboss.logging.Logger;
import org.jboss.logging.Messages;

/**
 * A support utility for creating message loggers and message bundle types. This supports using JBoss Logging 3.5.x and
 * 3.6.x.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@SuppressWarnings("removal")
public class LoggingSupport {
    private static final MethodHandle getMessageLogger;
    private static final MethodHandle getBundle;

    static {
        MethodHandle messageLogger = null;
        MethodHandle messageBundle = null;
        try {
            messageLogger = MethodHandles.publicLookup()
                    .findStatic(Logger.class, "getMessageLogger",
                            MethodType.methodType(Object.class, MethodHandles.Lookup.class, Class.class, String.class));
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
        }
        try {
            messageBundle = MethodHandles.publicLookup()
                    .findStatic(Messages.class, "getBundle",
                            MethodType.methodType(Object.class, MethodHandles.Lookup.class, Class.class));
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
        }
        getMessageLogger = messageLogger;
        getBundle = messageBundle;
    }

    /**
     * Creates a message logger instance for the specified logger interface.
     * <p>
     * If JBoss Logging 3.6.x or later is available, this method uses the
     * {@link Logger#getMessageLogger(MethodHandles.Lookup, Class, String)} method. Otherwise, it falls back to the
     * deprecated {@link Logger#getMessageLogger(Class, String)} method for compatibility with JBoss Logging 3.5.x.
     * </p>
     *
     * @param <T>         the logger interface type
     * @param lookup      the lookup to use, if applicable, for the message logger
     * @param loggerClass the logger interface class
     * @param loggerName  the logger category name
     *
     * @return a message logger instance
     */
    public static <T> T getMessageLogger(final MethodHandles.Lookup lookup, final Class<T> loggerClass,
            final String loggerName) {
        try {
            if (getMessageLogger != null) {
                return loggerClass.cast(getMessageLogger.invokeWithArguments(lookup, loggerClass, loggerName));
            }
            // Use the old method
            return Logger.getMessageLogger(loggerClass, loggerName);
        } catch (Throwable t) {
            // Fallback
            return Logger.getMessageLogger(loggerClass, loggerName);
        }
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
     * @param lookup      the lookup to use, if applicable, for the message bundle
     * @param bundleClass the bundle interface class
     *
     * @return a message bundle instance
     */
    public static <T> T getBundle(final MethodHandles.Lookup lookup, final Class<T> bundleClass) {
        try {
            if (getBundle != null) {
                return bundleClass.cast(getBundle.invokeWithArguments(lookup, bundleClass));
            }
            // Use the old method
            return Messages.getBundle(bundleClass);
        } catch (Throwable t) {
            // Fallback
            return Messages.getBundle(bundleClass);
        }
    }
}
