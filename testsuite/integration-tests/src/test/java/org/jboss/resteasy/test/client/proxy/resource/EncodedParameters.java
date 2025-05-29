/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2025 Red Hat, Inc., and individual contributors
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

package org.jboss.resteasy.test.client.proxy.resource;

import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.MatrixParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("param")
public interface EncodedParameters {

    @GET
    @Path("/path/{value}")
    String path(@PathParam("value") String value);

    @GET
    @Path("/path-encoded/{value}")
    String pathEncoded(@Encoded @PathParam("value") String value);

    @GET
    @Path("/query")
    String query(@QueryParam("value") String value);

    @GET
    @Path("/query-encoded")
    String queryEncoded(@QueryParam("value") String value);

    @GET
    @Path("/matrix")
    String matrix(@MatrixParam("value") String value);

    @GET
    @Path("/matrix-encoded")
    String matrixEncoded(@Encoded @MatrixParam("value") String value);

    @POST
    @Path("/form")
    String form(@FormParam("value") String value);

    @POST
    @Path("/form-encoded")
    String formEncoded(@Encoded @FormParam("value") String value);
}
