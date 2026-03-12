/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.cdi.injection.resource;

import jakarta.enterprise.context.RequestScoped;

/**
 * Simple CDI service to be injected into resources.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RequestScoped
public class CdiConstructorInjectionService {

    public String getMessage() {
        return "Hello from CDI service";
    }
}
