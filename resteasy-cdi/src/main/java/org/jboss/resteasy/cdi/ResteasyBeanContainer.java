/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.cdi;

/**
 * A container for CDI-managed Jakarta REST components discovered during processing. This is a read-only view of the
 * components that is populated by the {@link ResteasyCdiExtension}.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@FunctionalInterface
public interface ResteasyBeanContainer {

    /**
     * Checks if the type is manged by the CDI container.
     *
     * @param type the type to check
     *
     * @return {@code true} if the resource exists in this container, otherwise {@code false}
     */
    boolean contains(Class<?> type);
}
