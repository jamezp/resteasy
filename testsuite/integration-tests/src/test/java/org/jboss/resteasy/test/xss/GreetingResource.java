/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.resteasy.test.xss;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.owasp.encoder.Encode;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/greet")
// TODO (jrp) rename this
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/param/{name}")
    public Response helloGet(@PathParam("name") final String name) {

        final String encoded = Encode.forHtml(name);
        return createResponse(name);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("/query")
    public Response helloGetQuery(@QueryParam("name") final String name) {
        return createResponse(name);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.TEXT_HTML)
    @Path("/html/{name}")
    public Response helloGetHtml(@PathParam("name") final String name) {
        return createResponse(name);
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path("/param/post/{name}")
    public Response helloPost(@PathParam("name") final String name) {
        return createResponse(name);
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes({MediaType.TEXT_PLAIN, MediaType.TEXT_HTML})
    @Path("/entity")
    public Response helloPostEntity(final String name) {
        return createResponse(name);
    }

    private Response createResponse(final String name) {
        return Response.ok(String.format("<h1>Hello %s</h1>", name))
                .build();
    }
}
