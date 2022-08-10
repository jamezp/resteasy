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

package org.jboss.resteasy.client.jaxrs.engines;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.X509TrustManager;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.jboss.resteasy.client.jaxrs.ResponseHeaders;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.i18n.LogMessages;
import org.jboss.resteasy.client.jaxrs.i18n.Messages;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.client.jaxrs.spi.ClientConfigProvider;
import org.jboss.resteasy.concurrent.ContextualExecutors;
import org.jboss.resteasy.util.CaseInsensitiveMap;
import org.wildfly.security.ssl.SSLContextBuilder;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class HttpClientEngine implements AsyncClientHttpEngine {
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ResteasyClientBuilder resteasyClientBuilder;
    private final Executor defaultExecutor;
    private final Map<UUID, CompletableFuture<?>> queue = new ConcurrentHashMap<>();
    private final CookieManager cookieManager;
    private final ClientConfigProvider clientConfigProvider;

    public HttpClientEngine(final ResteasyClientBuilder resteasyClientBuilder,
                            final Executor defaultExecutor, final ClientConfigProvider clientConfigProvider) {
        this.resteasyClientBuilder = resteasyClientBuilder;
        this.defaultExecutor = defaultExecutor;
        this.cookieManager = new CookieManager();
        this.clientConfigProvider = clientConfigProvider;
    }

    @Override
    public SSLContext getSslContext() {
        return resteasyClientBuilder.getSSLContext();
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return (hostname, session) -> true;
    }

    @Override
    public Response invoke(final Invocation request) {
        final ClientInvocation clientInvocation = (ClientInvocation) request;
        try {
            final CompletableFuture<Response> cf = submit(defaultExecutor, clientInvocation, false, response -> response);
            try {
                return cf.get();
            } catch (InterruptedException e) {
                cf.cancel(true);
                throw new ProcessingException(Messages.MESSAGES.unableToInvokeRequest(e.toString()), e);
            } catch (ExecutionException e) {
                throw new ProcessingException(Messages.MESSAGES.unableToInvokeRequest(e.toString()), e);
            }
        } catch (Exception e) {
            LogMessages.LOGGER.clientSendProcessingFailure(e);
            throw new ProcessingException(Messages.MESSAGES.unableToInvokeRequest(e.toString()), e);
        }
    }

    @Override
    public boolean isFollowRedirects() {
        return resteasyClientBuilder.isFollowRedirects();
    }

    @Override
    public void setFollowRedirects(final boolean followRedirects) {
        resteasyClientBuilder.setFollowRedirects(followRedirects);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            final Iterator<Map.Entry<UUID, CompletableFuture<?>>> iterator = queue.entrySet().iterator();
            while (iterator.hasNext()) {
                final CompletableFuture<?> cf = iterator.next().getValue();
                LogMessages.LOGGER.warnf("Processing %s", cf);
                iterator.remove();
                if (!cf.isDone()) {
                    LogMessages.LOGGER.warnf("Cancelling %s", cf);
                    cf.cancel(true);
                    LogMessages.LOGGER.warnf("Cancelled %s", cf);
                }
            }
        }
    }

    @Override
    public <T> Future<T> submit(final ClientInvocation request, final boolean buffered,
                                final InvocationCallback<T> callback,
                                final ResultExtractor<T> extractor) {
        return submit(request, buffered, extractor)
                .whenComplete((response, error) -> {
                    if (callback != null) {
                        if (error != null) {
                            // TODO (jrp) may need to wrap the exception
                            callback.failed(error);
                        } else {
                            try {
                                callback.completed(response);
                            } catch (Throwable t) {
                                // TODO (jrp) this is what the ApacheHttpAsyncClient4Engine does, but it seems weird to me
                                LogMessages.LOGGER.exceptionIgnored(t);
                            } finally {
                                // TODO (jrp) this happens in the ApacheHttpAsyncClient4Engine, but I'm not sure why yet
                                if (response instanceof Response) {
                                    ((Response) response).close();
                                }
                            }
                        }
                    }
                });
    }

    @Override
    public <T> CompletableFuture<T> submit(final ClientInvocation request, final boolean buffered,
                                           final ResultExtractor<T> extractor) {
        return submit(request, buffered, extractor, null);
    }

    @Override
    public <T> CompletableFuture<T> submit(final ClientInvocation request, final boolean buffered,
                                           final ResultExtractor<T> extractor,
                                           final ExecutorService executorService) {
        try {
            return submit(executorService, request, buffered, extractor);
        } catch (Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }

    private <T> CompletableFuture<T> submit(final Executor executor, final ClientInvocation request,
                                            final boolean buffered,
                                            final ResultExtractor<T> extractor) {
        checkClosed();
        try {
            final HttpRequest httpRequest = createRequest(request);
            final HttpResponse.BodyHandler<InputStream> handler;
            if (buffered) {
                handler = HttpResponse.BodyHandlers.buffering(HttpResponse.BodyHandlers.ofInputStream(), Integer.MAX_VALUE);
            } else {
                handler = HttpResponse.BodyHandlers.ofInputStream();
            }
            return queue(createClient(executor, request.getUri()).sendAsync(httpRequest, handler)
                    .handle((response, error) -> {
                        if (error != null) {
                            Throwable cause = error;
                            if (cause instanceof CompletionException && cause.getCause() != null) {
                                cause = cause.getCause();
                            }
                            throw new ProcessingException(Messages.MESSAGES.unableToInvokeRequest(cause.toString()), cause);
                        }
                        return response;
                    })
                    .thenApply((response) -> extractor.extractResult(createResponse(request, response))));
        } catch (Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }

    private HttpClient createClient(final Executor executor, final URI uri) throws Exception {
        final HttpClient.Builder builder = HttpClient.newBuilder()
                .followRedirects(resteasyClientBuilder.isFollowRedirects() ? HttpClient.Redirect.ALWAYS : HttpClient.Redirect.NEVER);
        // TODO (jrp) what do we do here?
        HostnameVerifier verifier = null;
        if (resteasyClientBuilder.getHostnameVerifier() != null) {
            verifier = resteasyClientBuilder.getHostnameVerifier();
        } else {
            switch (resteasyClientBuilder.getHostnameVerification()) {
                case ANY:
                    verifier = new NoopHostnameVerifier();
                    break;
                case WILDCARD:
                    verifier = new DefaultHostnameVerifier();
                    break;
                case STRICT:
                    //this will load default file from httplcient.jar!/mozilla/public-suffix-list.txt
                    //if this default file isn't what you want, set a customized HostNameVerifier
                    //to ResteasyClientBuilder instead
                    // TODO (jrp) no, this is apache releated
                    verifier = new DefaultHostnameVerifier(PublicSuffixMatcherLoader.getDefault());
                    break;
            }
        }
        SSLContext sslContext = null;
        if (resteasyClientBuilder.isTrustManagerDisabled()) {
            sslContext = new SSLContextBuilder()
                    .setTrustManager(new PassthroughTrustManager())
                    .setClientMode(true)
                    .build()
                    .create();
        } else if (resteasyClientBuilder.getSSLContext() != null) {
            sslContext = resteasyClientBuilder.getSSLContext();
            if (!resteasyClientBuilder.getSniHostNames().isEmpty()) {
                final SSLParameters sslParameters = new SSLParameters();
                sslParameters.setServerNames(resteasyClientBuilder.getSniHostNames()
                        .stream()
                        .map(SNIHostName::new)
                        .collect(Collectors.toList())
                );
                builder.sslParameters(sslParameters);
            }
        } else if (resteasyClientBuilder.getKeyStore() != null || resteasyClientBuilder.getTrustStore() != null) {
            final KeyStore keyStore = resteasyClientBuilder.getKeyStore();
            final KeyStore trustStore = resteasyClientBuilder.getTrustStore();
            final SSLContextBuilder sslContextBuilder = new SSLContextBuilder()
                    .setClientMode(true);
            if (keyStore != null) {
                sslContextBuilder.setKeyManager(HttpSslUtils.getKeyManager(keyStore, resteasyClientBuilder.getKeyStorePassword()));
            }
            if (trustStore != null) {
                final X509TrustManager trustManager;
                if (resteasyClientBuilder.isTrustSelfSignedCertificates()) {
                    trustManager = new HostVerificationTrustManager(new TrustSelfSignedTrustManager(HttpSslUtils.getTrustManager(trustStore)), verifier);
                } else {
                    trustManager = new HostVerificationTrustManager(HttpSslUtils.getTrustManager(trustStore), verifier);
                }
                sslContextBuilder.setTrustManager(trustManager);
            }
            sslContext = sslContextBuilder.build()
                    .create();
        } else if (clientConfigProvider != null) {
            sslContext = clientConfigProvider.getSSLContext(uri);
        }
        if (sslContext != null) {
            builder.sslContext(sslContext);
        }
        if (resteasyClientBuilder.isCookieManagementEnabled()) {
            builder.cookieHandler(cookieManager);
        }
        if (resteasyClientBuilder.getDefaultProxyHostname() != null) {
            builder.proxy(ProxySelector.of(InetSocketAddress.createUnresolved(resteasyClientBuilder.getDefaultProxyHostname(), resteasyClientBuilder.getDefaultProxyPort())));
        }
        final long connectionTimeout = resteasyClientBuilder.getConnectionCheckoutTimeout(TimeUnit.MILLISECONDS);
        if (connectionTimeout > 1) {
            builder.connectTimeout(Duration.ofMillis(connectionTimeout));
        }
        // TODO (jrp) we need a way to set this
        builder.version(HttpClient.Version.HTTP_1_1);
        if (executor != null) {
            builder.executor(ContextualExecutors.wrap(executor));
        } else {
            builder.executor(defaultExecutor);
        }
        return builder.build();
    }

    private <T> CompletableFuture<T> queue(final CompletableFuture<T> cf) {
        final UUID uuid = UUID.randomUUID();
        if (!cf.isDone()) {
            // TODO (jrp) we really should use putIfAbsent and validate the UUID is unique
            queue.put(uuid, cf);
        }
        return cf.whenComplete((value, t) -> queue.remove(uuid));
    }

    private void checkClosed() {
        if (closed.get()) {
            throw new ProcessingException(Messages.MESSAGES.clientIsClosed());
        }
    }

    private HttpRequest createRequest(final ClientInvocation clientInvocation) throws IOException {
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(clientInvocation.getUri());
        final long readTimeout = resteasyClientBuilder.getReadTimeout(TimeUnit.MILLISECONDS);
        if (readTimeout > 1) {
            requestBuilder.timeout(Duration.ofMillis(readTimeout));
        }
        final String method = clientInvocation.getMethod();
        if ("GET".equalsIgnoreCase(method)) {
            // TODO (jrp) need to figure out why -1 is expected for a GET method
            requestBuilder.method("GET", DelegatingBodyPublisher.of(HttpRequest.BodyPublishers.noBody()));
        } else {
            if (clientInvocation.getEntity() != null) {
                // TODO (jrp) the threshold should be configurable
                final EntityOutputStream out = new EntityOutputStream(1024, () -> "resteasy-" + clientInvocation + "-" + method);
                // TODO (jrp) checkstyle chokes on this and throws an NPE, there might be a better way to handle it
                //try (out) {
                try {
                    clientInvocation.getDelegatingOutputStream().setDelegate(out);
                    clientInvocation.writeRequestBody(clientInvocation.getEntityStream());
                } finally {
                    out.close();
                }
                requestBuilder.method(method, out.toPublisher());
            } else {
                requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            }
        }
        // Add the headers
        final MultivaluedMap<String, String> headers = clientInvocation.getHeaders().asMap();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                if (value != null) {
                    requestBuilder.header(entry.getKey(), value);
                }
            }
        }
        return requestBuilder.build();
    }

    private static ClientResponse createResponse(final ClientInvocation clientInvocation,
                                                 final HttpResponse<InputStream> response) {
        // TODO (jrp) how do we add this to the ResourceCleaner
        final ClientResponse clientResponse = new ClientResponse(clientInvocation.getClientConfiguration(), clientInvocation.getTracingLogger()) {

            @Override
            public void releaseConnection(final boolean consumeInputStream) throws IOException {
                final InputStream in = getInputStream();
                if (in != null) {
                    in.close();
                    super.setInputStream(null);
                }
            }

            @Override
            protected void setInputStream(final InputStream is) {
                super.setInputStream(is);
                resetEntity();
            }

            @Override
            protected InputStream getInputStream() {
                final InputStream is = this.is;
                // TODO (jrp) is this right?
                if (is != null || isClosed()) {
                    return is;
                }
                // TODO (jrp) The Apache Client has Response.getEntity(). Sometimes this returns null which means there
                // TODO (jrp) is no body. However, response.body() only returns null if Response.previousResponse() is
                // TODO (jrp) invoked. We need to way to say there is no body vs an empty body which may not be possible.
                // TODO (jrp) If it's not possible, we may need to handle special cases where no body is allowed.
                // TODO (jrp) we need to determine if there is an entity, however the response.body() should never be null
                if (HttpMethod.HEAD.equals(response.request().method())) {
                    return null;
                }
                final InputStream body = response.body();
                super.setInputStream(body);
                return body;
            }
        };
        clientResponse.setProperties(clientInvocation.getMutableProperties());
        final Response.Status status = Response.Status.fromStatusCode(response.statusCode());
        if (status == null) {
            clientResponse.setStatus(response.statusCode());
        } else {
            clientResponse.setStatus(status.getStatusCode());
            clientResponse.setReasonPhrase(status.getReasonPhrase());
        }
        clientResponse.setHeaders(extractHeaders(response));
        clientResponse.setClientConfiguration(clientInvocation.getClientConfiguration());
        return clientResponse;
    }

    private static CaseInsensitiveMap<String> extractHeaders(final HttpResponse<?> response) {
        final CaseInsensitiveMap<String> headers = new CaseInsensitiveMap<>();
        response.headers().map().forEach((name, values) -> {
            for (String value : values) {
                headers.add(ResponseHeaders.lowerToDefault(name), value);
            }
        });
        return headers;
    }
}
