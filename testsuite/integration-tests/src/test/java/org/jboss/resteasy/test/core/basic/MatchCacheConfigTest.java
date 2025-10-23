/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test.core.basic;

import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.test.core.basic.resource.MatchCacheConfigResource;
import org.jboss.resteasy.utils.TestApplication;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Integration test for match cache configuration via Application properties.
 * Tests that RESTEASY_MATCH_CACHE_SIZE can be configured and that cache statistics
 * work correctly when enabled.
 *
 * @since RESTEasy 7.0.0
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class MatchCacheConfigTest {

    private static Client client;

    @ArquillianResource
    private URI baseUri;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, MatchCacheConfigTest.class.getSimpleName() + ".war")
                .addClasses(TestApplication.class, MatchCacheConfigResource.class)
                .addAsWebInfResource(TestUtil.createWebXml(null, null,
                        Map.of(ResteasyContextParameters.RESTEASY_MATCH_CACHE_SIZE, "100")), "web.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @BeforeAll
    public static void init() {
        client = ClientBuilder.newClient();
    }

    @AfterAll
    public static void after() throws Exception {
        client.close();
    }

    /**
     * Test that the match cache is working by making repeated requests.
     * The cache is configured with a custom size via Application.getProperties().
     */
    @Test
    public void testMatchCacheWithCustomSize() throws Exception {
        // Make multiple requests to the same resource to test caching
        for (int i = 0; i < 10; i++) {
            String response = client.target(TestUtil.generateUri(baseUri, "/test")).request().get(String.class);
            Assertions.assertEquals("Cache test successful", response);
        }

        // Test with path parameters
        for (int i = 0; i < 5; i++) {
            String response = client.target(TestUtil.generateUri(baseUri, "/test/" + i)).request().get(String.class);
            Assertions.assertEquals("Path: " + i, response);
        }
    }

    /**
     * Test that the cache configuration is accessible and correctly set.
     */
    @Test
    public void testMatchCacheConfiguration() throws Exception {
        // Verify the resource can access the configuration
        String response = client.target(TestUtil.generateUri(baseUri, "/test/config")).request().get(String.class);
        Assertions.assertTrue(response.contains("100"),
                "Expected cache size configuration to be 100, got: " + response);
    }

    /**
     * Verify that the match cache only caches METHOD ROUTING, not responses.
     * This is critical - the cache determines which Java method to invoke,
     * but does NOT cache the response. Each invocation should execute the method.
     */
    @Test
    public void testCacheDoesNotCacheResponses() throws Exception {
        // Reset counter
        client.target(TestUtil.generateUri(baseUri, "/test/reset")).request().get(String.class);

        // Make multiple requests to the counter endpoint
        // Even though routing is cached, each request should invoke the method
        for (int i = 1; i <= 5; i++) {
            String response = client.target(TestUtil.generateUri(baseUri, "/test/counter")).request().get(String.class);
            Assertions.assertEquals("Invocation: " + i, response,
                    "Each request should invoke the method even with cached routing");
        }
    }

    /**
     * SECURITY TEST: Verify that cache does not leak user context between requests.
     * The cache should only cache the routing decision (which method to call),
     * NOT user-specific data. Different users hitting the same endpoint should
     * see their own data, never another user's data.
     */
    @Test
    public void testCacheDoesNotLeakUserContext() throws Exception {
        // Reset counter
        client.target(TestUtil.generateUri(baseUri, "/test/reset")).request().get(String.class);

        // Simulate requests from different users
        // The cache should route to the same method, but each user should see their own identity
        String user1Response = client.target(TestUtil.generateUri(baseUri, "/test/user-specific"))
                .request()
                .header("X-User-Id", "user1")
                .get(String.class);
        Assertions.assertTrue(user1Response.contains("User: user1"),
                "User 1 should see their own identity, got: " + user1Response);
        Assertions.assertTrue(user1Response.contains("Invocation: 1"),
                "First invocation should be 1");

        String user2Response = client.target(TestUtil.generateUri(baseUri, "/test/user-specific"))
                .request()
                .header("X-User-Id", "user2")
                .get(String.class);
        Assertions.assertTrue(user2Response.contains("User: user2"),
                "User 2 should see their own identity, not user1's. Got: " + user2Response);
        Assertions.assertTrue(user2Response.contains("Invocation: 2"),
                "Second invocation should be 2, proving method was called again");

        String user3Response = client.target(TestUtil.generateUri(baseUri, "/test/user-specific"))
                .request()
                .header("X-User-Id", "user3")
                .get(String.class);
        Assertions.assertTrue(user3Response.contains("User: user3"),
                "User 3 should see their own identity. Got: " + user3Response);
        Assertions.assertTrue(user3Response.contains("Invocation: 3"),
                "Third invocation should be 3");

        // Make another request as user1 - should still work correctly
        String user1SecondResponse = client.target(TestUtil.generateUri(baseUri, "/test/user-specific"))
                .request()
                .header("X-User-Id", "user1")
                .get(String.class);
        Assertions.assertTrue(user1SecondResponse.contains("User: user1"),
                "User 1's second request should still see user1 identity");
        Assertions.assertTrue(user1SecondResponse.contains("Invocation: 4"),
                "Fourth invocation should be 4");
    }

    /**
     * Verify that query parameters don't affect cache routing.
     * The cache key does NOT include query parameters, only the path.
     * Different query params should route to the same method.
     */
    @Test
    public void testCacheWithDifferentQueryParameters() throws Exception {
        // All these requests should route to the same method (cached routing)
        // but should receive different query parameter values
        String response1 = client.target(TestUtil.generateUri(baseUri, "/test/query"))
                .queryParam("param", "value1")
                .request()
                .get(String.class);
        Assertions.assertEquals("Query param: value1", response1);

        String response2 = client.target(TestUtil.generateUri(baseUri, "/test/query"))
                .queryParam("param", "value2")
                .request()
                .get(String.class);
        Assertions.assertEquals("Query param: value2", response2,
                "Different query params should be processed correctly even with cached routing");

        String response3 = client.target(TestUtil.generateUri(baseUri, "/test/query"))
                .request()
                .get(String.class);
        Assertions.assertEquals("Query param: none", response3,
                "Missing query param should be handled correctly");
    }
}
