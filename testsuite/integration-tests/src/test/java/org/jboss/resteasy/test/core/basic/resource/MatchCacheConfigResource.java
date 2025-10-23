/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.core.basic.resource;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.config.ConfigurationFactory;

@Path("/test")
public class MatchCacheConfigResource {

    // Counter to track how many times the method is actually invoked
    // This helps us verify caching behavior - cache hits won't increment this
    private static final AtomicInteger invocationCounter = new AtomicInteger(0);
    private static final AtomicInteger userSpecificCounter = new AtomicInteger(0);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String basicTest() {
        return "Cache test successful";
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public String pathTest(@PathParam("id") String id) {
        return "Path: " + id;
    }

    @GET
    @Path("/config")
    @Produces(MediaType.TEXT_PLAIN)
    public String getConfig() {
        // Verify the configuration is accessible
        return "Cache size: " + ConfigurationFactory.getInstance()
                .getConfiguration()
                .getOptionalValue(ResteasyContextParameters.RESTEASY_MATCH_CACHE_SIZE, String.class)
                .orElse("default");
    }

    /**
     * Test endpoint that returns different values based on invocation count.
     * The cache should only cache the METHOD ROUTING, not the response.
     * So each call should increment the counter and return a different value.
     */
    @GET
    @Path("/counter")
    @Produces(MediaType.TEXT_PLAIN)
    public String testCounter() {
        int count = invocationCounter.incrementAndGet();
        return "Invocation: " + count;
    }

    /**
     * Test endpoint to verify cache doesn't leak user context.
     * Each call should see its own user identity regardless of cache.
     */
    @GET
    @Path("/user-specific")
    @Produces(MediaType.TEXT_PLAIN)
    public String testUserSpecific(@HeaderParam("X-User-Id") String userId, @Context SecurityContext securityContext) {
        int count = userSpecificCounter.incrementAndGet();
        String principal = securityContext.getUserPrincipal() != null
                ? securityContext.getUserPrincipal().getName()
                : "anonymous";
        return "User: " + (userId != null ? userId : principal) + ", Invocation: " + count;
    }

    /**
     * Test endpoint with query parameters.
     * Cache should route to this method regardless of query params.
     */
    @GET
    @Path("/query")
    @Produces(MediaType.TEXT_PLAIN)
    public String testQueryParams(@QueryParam("param") String param) {
        return "Query param: " + (param != null ? param : "none");
    }

    /**
     * Reset counters for testing
     */
    @GET
    @Path("/reset")
    @Produces(MediaType.TEXT_PLAIN)
    public String resetCounters() {
        invocationCounter.set(0);
        userSpecificCounter.set(0);
        return "Counters reset";
    }
}
