/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;

/**
 * Indicates that a field for parameter is a {@link jakarta.ws.rs.client.WebTarget} and optionally appends to the
 * non-empty path to the target URI.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Inherited
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestTarget {

    /**
     * The relative path the {@link jakarta.ws.rs.client.Client#target(URI)}.
     *
     * @return the relative path
     */
    String value() default "";
}
