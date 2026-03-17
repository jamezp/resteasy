/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.cdi.injection.provider.resource;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Simple echo service for CDI injection testing.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ApplicationScoped
public class EchoService {

    public String echo(final String message) {
        return String.format("echo: %s", message);
    }
}
