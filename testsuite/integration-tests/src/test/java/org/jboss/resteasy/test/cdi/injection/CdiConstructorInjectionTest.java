/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.cdi.injection;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.test.cdi.injection.resource.CdiConstructorInjectionResource;
import org.jboss.resteasy.test.cdi.injection.resource.CdiConstructorInjectionService;
import org.jboss.resteasy.utils.TestApplication;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.arquillian.junit.annotations.RequiresModule;

/**
 * Test that CDI resources can use constructor injection without requiring a public no-arg constructor.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ExtendWith(ArquillianExtension.class)
@RequiresModule(value = "org.jboss.resteasy.resteasy-cdi", minVersion = "7.0.2.Final")
public class CdiConstructorInjectionTest {

    @ArquillianResource
    private URI baseUri;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "cdi-constructor-injection.war")
                .addClasses(
                        TestApplication.class,
                        CdiConstructorInjectionResource.class,
                        CdiConstructorInjectionService.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * Test that a CDI resource with constructor injection works. The resource has an @Inject constructor with a CDI
     * bean parameter, and no public no-arg constructor.
     */
    @Test
    public void constructorInjection() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            final Response response = client.target(TestUtil.generateUri(baseUri, "cdi-constructor-injection/service"))
                    .request()
                    .get();

            final String result = response.readEntity(String.class);
            Assertions.assertEquals(200, response.getStatus(), () -> String.format("Response message: %s", result));
            Assertions.assertEquals("Service message: Hello from CDI service", result);

        }
    }

    /**
     * Tests that {@link jakarta.ws.rs.core.Context @Context} injection also works.
     */
    @Test
    public void multipleConstructorParameters() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            final Response response = client.target(TestUtil.generateUri(baseUri, "cdi-constructor-injection/context"))
                    .request()
                    .get();

            final String result = response.readEntity(String.class);
            Assertions.assertEquals(200, response.getStatus(), () -> String.format("Response message: %s", result));
            Assertions.assertEquals("Path: /cdi-constructor-injection/context", result);

        }
    }
}
