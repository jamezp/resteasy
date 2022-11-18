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

package org.jboss.resteasy.specimpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.core.se.ResteasySeConfiguration;
import org.jboss.resteasy.core.se.ResteasySeInstance;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.PriorityServiceLoader;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ResteasyRuntimeDelegate extends RuntimeDelegate {
    @Override
    public UriBuilder createUriBuilder() {
        return new ResteasyUriBuilderImpl();
    }

    @Override
    public Response.ResponseBuilder createResponseBuilder() {
        return new ResponseBuilderImpl();
    }

    @Override
    public Variant.VariantListBuilder createVariantListBuilder() {
        return new VariantListBuilderImpl();
    }

    @Override
    public <T> T createEndpoint(final Application application, final Class<T> endpointType)
            throws IllegalArgumentException, UnsupportedOperationException {
        if (application == null)
            throw new IllegalArgumentException(Messages.MESSAGES.applicationParamNull());
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> HeaderDelegate<T> createHeaderDelegate(final Class<T> type) throws IllegalArgumentException {
        // TODO (jrp) implement this
        return null;
    }

    @Override
    public Link.Builder createLinkBuilder() {
        return new LinkBuilderImpl();
    }

    @Override
    public SeBootstrap.Configuration.Builder createConfigurationBuilder() {
        return ResteasySeConfiguration.builder();
    }

    @Override
    public CompletionStage<SeBootstrap.Instance> bootstrap(final Application application,
                                                           final SeBootstrap.Configuration configuration) {
        return ResteasySeInstance.create(Objects.requireNonNull(application, Messages.MESSAGES.nullParameter("application")),
                configuration);
    }

    @Override
    public CompletionStage<SeBootstrap.Instance> bootstrap(final Class<? extends Application> clazz,
                                                           final SeBootstrap.Configuration configuration) {
        return ResteasySeInstance.create(Objects.requireNonNull(clazz, Messages.MESSAGES.nullParameter("clazz")),
                configuration);
    }

    @Override
    public EntityPart.Builder createEntityPartBuilder(final String partName) throws IllegalArgumentException {
        if (partName == null) {
            throw new IllegalArgumentException(Messages.MESSAGES.nullParameter("partName"));
        }
        final Function<Class<? extends EntityPart.Builder>, EntityPart.Builder> constructor = builderClass -> {
            try {
                final Constructor<? extends EntityPart.Builder> c = builderClass.getConstructor(String.class);
                return c.newInstance(partName);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                throw Messages.MESSAGES.failedToConstructClass(e, builderClass);
            }
        };
        final Optional<EntityPart.Builder> found;
        if (System.getSecurityManager() == null) {
            found = PriorityServiceLoader.load(EntityPart.Builder.class, constructor)
                    .first();
        } else {
            found = AccessController.doPrivileged((PrivilegedAction<Optional<EntityPart.Builder>>) () -> PriorityServiceLoader.load(EntityPart.Builder.class, constructor)
                    .first());
        }
        return found.orElseThrow(() -> Messages.MESSAGES.noImplementationFound(EntityPart.Builder.class.getName()));
    }
}
