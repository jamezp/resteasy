/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.cdi.injection.scope;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.test.cdi.injection.scope.resource.ApplicationScopedResource;
import org.jboss.resteasy.test.cdi.injection.scope.resource.CounterService;
import org.jboss.resteasy.utils.TestApplication;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.arquillian.junit.annotations.RequiresModule;

/**
 * Test that @ApplicationScoped CDI resources work correctly with constructor injection and maintain singleton behavior
 * across requests.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ExtendWith(ArquillianExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@RequiresModule(value = "org.jboss.resteasy.resteasy-cdi", minVersion = "6.2.16.Final")
public class ApplicationScopedResourceTest {

    @ArquillianResource
    private URI baseUri;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "app-scoped-resource.war")
                .addClasses(
                        TestApplication.class,
                        ApplicationScopedResource.class,
                        CounterService.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    /**
     * Test that @ApplicationScoped resource maintains state across requests (singleton behavior).
     */
    @Test
    @Order(1)
    public void applicationScopedIsSingletonIncrement() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            final WebTarget incrementTarget = client.target(TestUtil.generateUri(baseUri, "app-scoped/increment"));
            // First request
            final int first = incrementTarget.request().get(Integer.class);

            // Second request - should get incremented value from same instance
            final int second = incrementTarget.request().get(Integer.class);

            // Third request
            final int third = incrementTarget.request().get(Integer.class);

            // Verify singleton behavior - each request increments the same instance's counter
            Assertions.assertEquals(1, first, "First request should return 1");
            Assertions.assertEquals(2, second, "Second request should return 2 (same instance)");
            Assertions.assertEquals(3, third, "Third request should return 3 (same instance)");
        }
    }

    /**
     * Tests that the current count is 3 given the test above that has executed
     */
    @Test
    @Order(2)
    public void applicationScopedIsSingletonGet() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            final int result = client.target(TestUtil.generateUri(baseUri, "app-scoped/current"))
                    .request()
                    .get(Integer.class);

            Assertions.assertEquals(3, result, "Expected a result of 3 in the application scoped resource");
        }
    }

    /**
     * Tests that the @ApplicationSoped resource with a @RequestScoped service, that the service is new for each
     * request.
     */
    @Test
    public void requestScopedServiceIncrement() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            final WebTarget incrementTarget = client.target(TestUtil.generateUri(baseUri, "app-scoped/service/increment"));
            // First request
            final int first = incrementTarget.request().get(Integer.class);

            // Second request
            final int second = incrementTarget.request().get(Integer.class);

            // Third request
            final int third = incrementTarget.request().get(Integer.class);

            // Verify singleton behavior - each request increments the same instance's counter
            Assertions.assertEquals(1, first, "First request should return 1");
            Assertions.assertEquals(1, second, "Second request should return 1");
            Assertions.assertEquals(1, third, "Third request should return 1");
        }
    }

    /**
     * Tests that the current count from the service should always be 0.
     */
    @Test
    public void requestScopedServiceGet() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            final int result = client.target(TestUtil.generateUri(baseUri, "app-scoped/service/current"))
                    .request()
                    .get(Integer.class);

            Assertions.assertEquals(0, result,
                    "Expected a result of 0 in the application scoped resource, with a request scoped service");
        }
    }

    @Test
    public void contextUriPath() throws Exception {
        final String expectedPath = "/app-scoped/context/path";
        try (Client client = ClientBuilder.newClient()) {
            final String result = client.target(TestUtil.generateUri(baseUri, expectedPath))
                    .request()
                    .get(String.class);

            Assertions.assertEquals(expectedPath, result);
        }
    }

    @Test
    public void contextParamUriPath() throws Exception {
        final String expectedPath = "/app-scoped/context-param/path";
        try (Client client = ClientBuilder.newClient()) {
            final String result = client.target(TestUtil.generateUri(baseUri, expectedPath))
                    .request()
                    .get(String.class);

            Assertions.assertEquals(expectedPath, result);
        }
    }

    @Test
    public void injectUriPath() throws Exception {
        final String expectedPath = "/app-scoped/inject/path";
        try (Client client = ClientBuilder.newClient()) {
            final String result = client.target(TestUtil.generateUri(baseUri, expectedPath))
                    .request()
                    .get(String.class);

            Assertions.assertEquals(expectedPath, result);
        }
    }
}
