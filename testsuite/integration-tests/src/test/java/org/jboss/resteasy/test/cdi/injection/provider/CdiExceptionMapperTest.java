/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.cdi.injection.provider;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.test.cdi.injection.provider.resource.CountingExceptionMapper;
import org.jboss.resteasy.test.cdi.injection.provider.resource.CustomException;
import org.jboss.resteasy.test.cdi.injection.provider.resource.EchoService;
import org.jboss.resteasy.test.cdi.injection.provider.resource.ExceptionResource;
import org.jboss.resteasy.utils.TestApplication;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.arquillian.junit.annotations.RequiresModule;

/**
 * Test that CDI providers (ExceptionMapper) can use constructor injection.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ExtendWith(ArquillianExtension.class)
@RequiresModule(value = "org.jboss.resteasy.resteasy-cdi", minVersion = "7.0.2.Final")
public class CdiExceptionMapperTest {

    @ArquillianResource
    private URI baseUri;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "cdi-exception-mapper.war")
                .addClasses(
                        TestApplication.class,
                        ExceptionResource.class,
                        CountingExceptionMapper.class,
                        CustomException.class,
                        EchoService.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * Test that ExceptionMapper with constructor injection works.
     */
    @Test
    public void exceptionMapperWithConstructorInjection() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            final Response response = client.target(TestUtil.generateUri(baseUri, "exception/throw"))
                    .request()
                    .get();

            final String entity = response.readEntity(String.class);

            // ExceptionMapper should map to 400
            Assertions.assertEquals(400, response.getStatus(),
                    () -> String.format("Response should have thrown an error but ws %s", entity));

            // Should contain the custom message with logging service prefix
            Assertions.assertEquals("echo: Custom exception occurred", entity);
        }
    }

    /**
     * Tests that the ExceptionMapper inject and the ExceptionMapper looked up from the Providers are the same instance.
     * We run this test twice to ensure the same instance is used in multiple requests.
     */
    @RepeatedTest(2)
    public void mappersEqual() throws Exception {
        try (
                Client client = ClientBuilder.newClient();
                Response response = client.target(TestUtil.generateUri(baseUri, "exception/check-mapper"))
                        .request()
                        .get()) {

            Assertions.assertEquals(200, response.getStatus(),
                    () -> String.format("Response should have thrown an error but ws %s", response.readEntity(String.class)));
            Assertions.assertTrue(response.readEntity(Boolean.class));
        }
    }
}
