/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2024 Red Hat, Inc., and individual contributors
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

package org.jboss.resteasy.test.common;

import java.lang.reflect.Field;
import java.util.function.Predicate;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.UriBuilder;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.ReflectionUtils;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class TestHttpServerExtension implements BeforeAllCallback, BeforeEachCallback, ParameterResolver {

    private static final String SERVER_KEY = "test.server";
    private static final String CLIENT_KEY = "test.client";
    private static final ExtensionContext.Namespace SERVER_NAMESPACE = ExtensionContext.Namespace.create("Test.Server");

    @Override
    public void beforeAll(final ExtensionContext context) {
        injectFields(context, null, getOrCreateServer(context), ReflectionUtils::isStatic);
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        context.getRequiredTestInstances().getAllInstances() //
                .forEach(instance -> injectFields(context, instance, getOrCreateServer(context), ReflectionUtils::isNotFinal));
    }

    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return (parameterContext.isAnnotated(RequestTarget.class)
                && WebTarget.class.isAssignableFrom(parameterContext.getParameter().getType()));
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.isAnnotated(RequestTarget.class)
                && WebTarget.class.isAssignableFrom(parameterContext.getParameter().getType())) {
            final TestHttpServer testServer = getOrCreateServer(extensionContext);
            final Client client = getOrCreateClient(extensionContext);
            final UriBuilder uriBuilder = UriBuilder.fromUri(testServer.baseUri())
                    .uri(parameterContext.getParameter().getAnnotation(RequestTarget.class).value());
            return client.target(uriBuilder.build());
        }
        return null;
    }

    private void injectFields(final ExtensionContext context, final Object instance, final TestHttpServer testServer,
            final Predicate<Field> filter) {
        AnnotationUtils.findAnnotatedFields(context.getRequiredTestClass(), RequiresHttpServer.class, filter)
                .forEach(field -> {
                    if (ReflectionUtils.isFinal(field)) {
                        throw new ExtensionConfigurationException(
                                String.format("@RequiresHttpServer field %s.%s cannot be declared final.",
                                        context.getTestClass().map(Class::getName).orElse(""), field));
                    }
                    try {
                        ReflectionUtils.makeAccessible(field)
                                .set(instance, testServer);
                    } catch (IllegalAccessException e) {
                        throw new ExtensionConfigurationException(String.format("Failed to set the field %s.%s.",
                                context.getTestClass().map(Class::getName).orElse(""), field), e);
                    }
                });
    }

    @SuppressWarnings("resource")
    private static TestHttpServer getOrCreateServer(final ExtensionContext context) {
        final ExtensionContext.Store store = getGlobalStore(context);
        return store.getOrComputeIfAbsent(SERVER_KEY, key -> new TestHttpServer().start(), TestHttpServer.class);
    }

    private static Client getOrCreateClient(final ExtensionContext context) {
        final ExtensionContext.Store store = getGlobalStore(context);
        return store.getOrComputeIfAbsent(CLIENT_KEY, key -> ClientBuilder.newClient(), Client.class);
    }

    private static ExtensionContext.Store getGlobalStore(final ExtensionContext context) {
        return context.getRoot().getStore(ExtensionContext.StoreScope.LAUNCHER_SESSION, SERVER_NAMESPACE);
    }
}
