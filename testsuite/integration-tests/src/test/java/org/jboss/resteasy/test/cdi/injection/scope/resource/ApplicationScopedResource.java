/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.cdi.injection.scope.resource;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

/**
 * An {@link ApplicationScoped }@ApplicationScoped} resource with constructor injection. Should maintain singleton
 * behavior.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ApplicationScoped
@Path("/app-scoped")
@Produces(MediaType.TEXT_PLAIN)
public class ApplicationScopedResource {

    private final AtomicInteger counter = new AtomicInteger(0);
    private final CounterService service;

    @Context
    private UriInfo contextUriInfo;

    @Inject
    private UriInfo injectedUriInfo;

    /**
     * The {@link @Path} annotation makes this a {@link jakarta.enterprise.context.ApplicationScoped} bean which means we
     * need a no-arg constructor.
     */
    @SuppressWarnings("unused")
    ApplicationScopedResource() {
        service = null;
    }

    @Inject
    ApplicationScopedResource(final CounterService service) {
        this.service = service;
    }

    @GET
    @Path("/increment")
    public int incrementCounter() {
        return counter.incrementAndGet();
    }

    @GET
    @Path("/current")
    public int getCounter() {
        return counter.get();
    }

    @GET
    @Path("/service/increment")
    public int serviceIncrementCounter() {
        return service.increment();
    }

    @GET
    @Path("/service/current")
    public int serviceGetCounter() {
        return service.get();
    }

    @GET
    @Path("context/path")
    public String contextPath() {
        return contextUriInfo.getPath();
    }

    @GET
    @Path("context-param/path")
    public String contextPath(@Context final UriInfo uriInfo) {
        return uriInfo.getPath();
    }

    @GET
    @Path("inject/path")
    public String injectPath() {
        return injectedUriInfo.getPath();
    }
}
