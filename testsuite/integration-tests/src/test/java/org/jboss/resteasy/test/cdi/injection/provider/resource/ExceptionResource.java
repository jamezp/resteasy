/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.cdi.injection.provider.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Providers;

/**
 * Resource that throws CustomException to test ExceptionMapper.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Path("/exception")
@Produces(MediaType.TEXT_PLAIN)
public class ExceptionResource {

    @Inject
    private Providers providers;

    @Inject
    private CountingExceptionMapper exceptionMapper;

    @GET
    @Path("/throw")
    public String throwException() {
        throw new CustomException("Custom exception occurred");
    }

    @GET
    @Path("/check-mapper")
    public boolean mapper() {
        final ExceptionMapper<CustomException> mapper = providers.getExceptionMapper(CustomException.class);
        if (mapper == null) {
            throw new BadRequestException("Mapper not found");
        }
        // This is an ApplicationScoped bean so it should be the same instance
        if (mapper == exceptionMapper) {
            return true;
        }
        throw new BadRequestException(
                String.format("Injected exception mapper %s is not equal to the exception mapper %s looked up in the provider.",
                        exceptionMapper, mapper));
    }
}
