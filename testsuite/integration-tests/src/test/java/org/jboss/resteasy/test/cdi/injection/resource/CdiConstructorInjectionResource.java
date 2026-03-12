/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.cdi.injection.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

/**
 * Test resource demonstrating CDI constructor injection.
 * <p>
 * This resource does NOT have a no-arg constructor and uses @Inject on the constructor to inject CDI beans. This was
 * previously not possible and would fail with "Unable to find public constructor".
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Path("/cdi-constructor-injection")
public class CdiConstructorInjectionResource {

    private final CdiConstructorInjectionService service;

    @Context
    private UriInfo uriInfo;

    /**
     * The {@link @Path} annotation makes this a {@link jakarta.enterprise.context.RequestScoped} bean which means we
     * need a no-arg constructor.
     */
    @SuppressWarnings("unused")
    CdiConstructorInjectionResource() {
        service = null;
    }

    /**
     * Constructor with CDI injection.
     *
     * @param service the CDI service to inject
     */
    @Inject
    public CdiConstructorInjectionResource(final CdiConstructorInjectionService service) {
        this.service = service;
    }

    @GET
    @Path("/service")
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        // Use constructor-injected service
        final String serviceMessage = service.getMessage();
        return "Service message: " + serviceMessage;
    }

    @GET
    @Path("/context")
    @Produces(MediaType.TEXT_PLAIN)
    public String testContext() {
        return "Path: " + uriInfo.getPath();
    }
}
