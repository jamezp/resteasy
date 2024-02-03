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

package dev.resteasy.providers.mutiny;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow.Publisher;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.RxInvoker;
import jakarta.ws.rs.client.SyncInvoker;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;
import org.jboss.resteasy.concurrent.ContextualExecutors;

import io.smallrye.mutiny.Multi;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class PublisherRxInvoker implements RxInvoker<Publisher<?>> {
    private static class WrappedClientInvocation extends ClientInvocation {
        protected WrappedClientInvocation(final ClientInvocation clientInvocation) {
            super(clientInvocation);
        }
    }

    private final ClientInvocationBuilder builder;
    private final ExecutorService executorService;

    public PublisherRxInvoker(final SyncInvoker invoker, final ExecutorService executorService) {
        this.builder = (ClientInvocationBuilder) invoker;
        // TODO (jrp) is this right?
        this.executorService = executorService == null ? ContextualExecutors.threadPool()
                : ContextualExecutors.wrap(executorService);
    }

    @Override
    public Publisher<Response> get() {
        return Multi.createFrom().completionStage(createClientInvocation(HttpMethod.GET, null).submitCF());

    }

    @Override
    public <R> Publisher<R> get(final Class<R> responseType) {
        return Multi.createFrom().completionStage(createClientInvocation(HttpMethod.GET, null).submitCF(responseType));
    }

    @Override
    public <R> Publisher<R> get(final GenericType<R> responseType) {
        return null;
    }

    @Override
    public Publisher<Response> put(final Entity<?> entity) {
        return null;
    }

    @Override
    public <R> Publisher<R> put(final Entity<?> entity, final Class<R> responseType) {
        return null;
    }

    @Override
    public <R> Publisher<R> put(final Entity<?> entity, final GenericType<R> responseType) {
        return null;
    }

    @Override
    public Publisher<Response> post(final Entity<?> entity) {
        return null;
    }

    @Override
    public <R> Publisher<R> post(final Entity<?> entity, final Class<R> responseType) {
        return null;
    }

    @Override
    public <R> Publisher<R> post(final Entity<?> entity, final GenericType<R> responseType) {
        return null;
    }

    @Override
    public Publisher<Response> delete() {
        return null;
    }

    @Override
    public <R> Publisher<R> delete(final Class<R> responseType) {
        return null;
    }

    @Override
    public <R> Publisher<R> delete(final GenericType<R> responseType) {
        return null;
    }

    @Override
    public Publisher<Response> head() {
        return null;
    }

    @Override
    public Publisher<Response> options() {
        return null;
    }

    @Override
    public <R> Publisher<R> options(final Class<R> responseType) {
        return null;
    }

    @Override
    public <R> Publisher<R> options(final GenericType<R> responseType) {
        return null;
    }

    @Override
    public Publisher<Response> trace() {
        return null;
    }

    @Override
    public <R> Publisher<R> trace(final Class<R> responseType) {
        return null;
    }

    @Override
    public <R> Publisher<R> trace(final GenericType<R> responseType) {
        return null;
    }

    @Override
    public Publisher<Response> method(final String name) {
        return null;
    }

    @Override
    public <R> Publisher<R> method(final String name, final Class<R> responseType) {
        return null;
    }

    @Override
    public <R> Publisher<R> method(final String name, final GenericType<R> responseType) {
        return null;
    }

    @Override
    public Publisher<Response> method(final String name, final Entity<?> entity) {
        return null;
    }

    @Override
    public <R> Publisher<R> method(final String name, final Entity<?> entity, final Class<R> responseType) {
        return null;
    }

    @Override
    public <R> Publisher<R> method(final String name, final Entity<?> entity, final GenericType<R> responseType) {
        return null;
    }

    private ClientInvocation createClientInvocation(final String method, final Entity<?> entity) {
        final ClientInvocation invoker = new WrappedClientInvocation(builder.getClientInvocation());
        invoker.setMethod(method);
        invoker.setEntity(entity);
        return invoker;
    }
}
