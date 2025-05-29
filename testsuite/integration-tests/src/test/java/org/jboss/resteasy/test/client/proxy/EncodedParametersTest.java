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

package org.jboss.resteasy.test.client.proxy;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.container.annotation.ArquillianTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.test.client.proxy.resource.EncodedParameters;
import org.jboss.resteasy.test.client.proxy.resource.EncodedParametersResource;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Tests that parameters passed to a proxy are properly encoded or not encoded based on the
 * {@link jakarta.ws.rs.Encoded @Encoded} annotation.
 * <p>
 * On client side, when a parameter is annotated with {@code @Encoded}, the parameter should not be re-encoded. On
 * the server side, when a parameter is annotated with {@code @Encoded}, the parameter should not be decoded.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ArquillianTest
@RunAsClient
public class EncodedParametersTest {

    @ArquillianResource
    private URI uri;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return TestUtil.prepareArchive(EncodedParametersTest.class.getSimpleName())
                .addClasses(
                        EncodedParameters.class,
                        EncodedParametersResource.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentsProvider.class)
    public void path(final String testValue) {
        try (Client client = ClientBuilder.newClient()) {
            final var encodedParameters = createClient(client);
            final var foundValue = encodedParameters.path(testValue);
            // A slash is a path separator. If not ended it acts as a path separate and results in it being stripped,
            // as expected, from the decoded parameter
            final var expectedValue = foundValue.charAt(0) == '/' ? foundValue.substring(1) : foundValue;
            Assertions.assertEquals(expectedValue, foundValue, () -> String.format("Test value was %s", testValue));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentsProvider.class)
    public void encodedPath(final String testValue) {
        try (Client client = ClientBuilder.newClient()) {
            final var encodedParameters = createClient(client);
            final var encodedValue = URLEncoder.encode(testValue, StandardCharsets.UTF_8);
            final var foundValue = encodedParameters.pathEncoded(encodedValue);
            Assertions.assertEquals(encodedValue, foundValue, () -> String.format("Test value was %s", testValue));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentsProvider.class)
    public void query(final String testValue) {
        try (Client client = ClientBuilder.newClient()) {
            final var encodedParameters = createClient(client);
            final var foundValue = encodedParameters.query(testValue);
            Assertions.assertEquals(testValue, foundValue, () -> String.format("Test value was %s", testValue));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentsProvider.class)
    public void encodedQuery(final String testValue) {
        try (Client client = ClientBuilder.newClient()) {
            final var encodedParameters = createClient(client);
            final var encodedValue = URLEncoder.encode(testValue, StandardCharsets.UTF_8);
            final var foundValue = encodedParameters.queryEncoded(encodedValue);
            Assertions.assertEquals(encodedValue, foundValue, () -> String.format("Test value was %s", testValue));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentsProvider.class)
    public void matrix(final String testValue) {
        try (Client client = ClientBuilder.newClient()) {
            final var encodedParameters = createClient(client);
            final var foundValue = encodedParameters.matrix(testValue);
            Assertions.assertEquals(testValue, foundValue, () -> String.format("Test value was %s", testValue));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentsProvider.class)
    public void encodedMatrix(final String testValue) {
        try (Client client = ClientBuilder.newClient()) {
            final var encodedParameters = createClient(client);
            final var encodedValue = URLEncoder.encode(testValue, StandardCharsets.UTF_8);
            final var foundValue = encodedParameters.matrixEncoded(encodedValue);
            Assertions.assertEquals(encodedValue, foundValue, () -> String.format("Test value was %s", testValue));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentsProvider.class)
    public void form(final String testValue) {
        try (Client client = ClientBuilder.newClient()) {
            final var encodedParameters = createClient(client);
            final var foundValue = encodedParameters.form(testValue);
            Assertions.assertEquals(testValue, foundValue, () -> String.format("Test value was %s", testValue));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(DefaultArgumentsProvider.class)
    public void encodedForm(final String testValue) {
        try (Client client = ClientBuilder.newClient()) {
            final var encodedParameters = createClient(client);
            final var encodedValue = URLEncoder.encode(testValue, StandardCharsets.UTF_8);
            final var foundValue = encodedParameters.formEncoded(encodedValue);
            Assertions.assertEquals(encodedValue, foundValue, () -> String.format("Test value was %s", testValue));
        }
    }

    private EncodedParameters createClient(final Client client) {
        Assertions.assertInstanceOf(ResteasyClient.class, client);
        return ((ResteasyClient) client).target(uri).proxyBuilder(EncodedParameters.class).build();
    }

    public static class DefaultArgumentsProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {
            return Stream.of("/value", "test%value", "test value", "test=value").map(Arguments::of);
        }
    }
}
