/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.cdi.injection.scope.resource;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.RequestScoped;

/**
 * A simple CDI service for injection testing.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RequestScoped
public class CounterService {
    private final AtomicInteger counter = new AtomicInteger();

    public int get() {
        return counter.get();
    }

    public int increment() {
        return counter.incrementAndGet();
    }
}
