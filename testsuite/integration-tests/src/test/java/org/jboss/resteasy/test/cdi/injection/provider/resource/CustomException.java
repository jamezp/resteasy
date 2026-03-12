/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.cdi.injection.provider.resource;

/**
 * Custom exception for testing ExceptionMapper.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class CustomException extends RuntimeException {

    public CustomException(final String message) {
        super(message);
    }
}
