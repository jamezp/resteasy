/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.cdi.injection.provider.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * An ExceptionMapper with CDI constructor injection.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Provider
public class CountingExceptionMapper implements ExceptionMapper<CustomException> {

    private final EchoService echo;

    /**
     * The {@link Provider @Provider} annotation makes this a {@link jakarta.enterprise.context.ApplicationScoped} bean
     * which means we need a no-arg constructor.
     */
    @SuppressWarnings("unused")
    CountingExceptionMapper() {
        echo = null;
    }

    @Inject
    CountingExceptionMapper(final EchoService echo) {
        this.echo = echo;
    }

    @Override
    public Response toResponse(final CustomException exception) {
        return Response.status(400).entity(echo.echo(exception.getMessage())).build();
    }
}
