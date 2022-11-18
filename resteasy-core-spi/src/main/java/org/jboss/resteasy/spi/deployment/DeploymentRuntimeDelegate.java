/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
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

package org.jboss.resteasy.spi.deployment;

import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class DeploymentRuntimeDelegate extends RuntimeDelegate {

    @Override
    public UriBuilder createUriBuilder() {
        return getDelegate().createUriBuilder();
    }

    @Override
    public Response.ResponseBuilder createResponseBuilder() {
        return getDelegate().createResponseBuilder();
    }

    @Override
    public Variant.VariantListBuilder createVariantListBuilder() {
        return getDelegate().createVariantListBuilder();
    }

    @Override
    public <T> T createEndpoint(final Application application, final Class<T> endpointType)
            throws IllegalArgumentException, UnsupportedOperationException {
        return getDelegate().createEndpoint(application, endpointType);
    }

    @Override
    public <T> HeaderDelegate<T> createHeaderDelegate(final Class<T> type) throws IllegalArgumentException {
        return getDelegate().createHeaderDelegate(type);
    }

    @Override
    public Link.Builder createLinkBuilder() {
        return getDelegate().createLinkBuilder();
    }

    @Override
    public SeBootstrap.Configuration.Builder createConfigurationBuilder() {
        return getDelegate().createConfigurationBuilder();
    }

    @Override
    public CompletionStage<SeBootstrap.Instance> bootstrap(final Application application,
                                                           final SeBootstrap.Configuration configuration) {
        return getDelegate().bootstrap(application, configuration);
    }

    @Override
    public CompletionStage<SeBootstrap.Instance> bootstrap(final Class<? extends Application> clazz,
                                                           final SeBootstrap.Configuration configuration) {
        return getDelegate().bootstrap(clazz, configuration);
    }

    @Override
    public EntityPart.Builder createEntityPartBuilder(final String partName) throws IllegalArgumentException {
        return getDelegate().createEntityPartBuilder(partName);
    }

    private static RuntimeDelegate getDelegate() {
        final DeploymentContext context = DeploymentContext.getDeploymentContext();
        return context.computeIfAbsent(ResteasyProviderFactory.class, ResteasyProviderFactory::getInstance);
    }
}
