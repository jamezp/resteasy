/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.client.engine.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.api.ClientBuilderConfiguration;
import org.jboss.resteasy.client.jaxrs.engines.AsyncClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.spi.EntityOutputStream;
import org.jboss.resteasy.spi.config.ConfigurationFactory;
import org.jboss.resteasy.util.CaseInsensitiveMap;

import dev.resteasy.client.engine.http._private.LogMessages;
import dev.resteasy.client.engine.http.config.HttpClientConfigurationOptions;

/**
 * Abstract base class for HTTP client engines backed by the JDK's {@link HttpClient}.
 * <p>
 * This class provides common request building, response handling, and async execution logic shared by
 * both standard and per-request engine implementations. Subclasses implement the template methods:
 * </p>
 * <ul>
 * <li>{@link #httpClient(URI)} - Returns the {@code HttpClient} to use for a given request URI</li>
 * <li>{@link #closeClient(HttpClient)} - Closes the specified client or performs cleanup</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
abstract class AbstractJavaClientHttpEngine implements ClientHttpEngine, AsyncClientHttpEngine {
    private static final Collection<String> NO_BODY_REQUEST_METHODS = List.of(
            "CONNECT",
            "GET",
            "HEAD");
    private static final Collection<String> NO_BODY_RESPONSE_METHODS = List.of(
            "HEAD"
    //"TRACE" the spec seems to allow TRACE methods to have a response body
    );
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ClientBuilderConfiguration configuration;

    AbstractJavaClientHttpEngine(final ClientBuilderConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public SSLContext getSslContext() {
        return configuration.sslContext();
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        // Not supported in the Java HTTP Client
        return null;
    }

    @Override
    public Response invoke(final Invocation request) {
        if (closed.get()) {
            throw LogMessages.LOGGER.clientIsClosed();
        }
        final ClientInvocation clientInvocation = (ClientInvocation) request;
        try {
            final HttpClient client = httpClient(clientInvocation.getUri());
            final HttpResponse<InputStream> response = client.send(createRequest(clientInvocation, client),
                    HttpResponse.BodyHandlers.ofInputStream());
            return createResponse(client, clientInvocation, response);
        } catch (Exception e) {
            LogMessages.LOGGER.debugf(e, "Failed to invoke %s", request);
            if (e instanceof ProcessingException) {
                throw ((ProcessingException) e);
            }
            throw LogMessages.LOGGER.unableToInvokeRequest(e, e.toString());
        }
    }

    @Override
    public boolean isFollowRedirects() {
        return configuration.isFollowRedirects();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            closeClient(null);
        }
    }

    @Override
    public <T> Future<T> submit(final ClientInvocation request, final boolean buffered, final InvocationCallback<T> callback,
            final ResultExtractor<T> extractor) {
        return submit(request, buffered, extractor)
                .whenComplete((response, error) -> {
                    if (callback != null) {
                        if (error != null) {
                            callback.failed(error);
                        } else {
                            try {
                                callback.completed(response);
                            } catch (Throwable t) {
                                LogMessages.LOGGER.debug("Ignoring exception thrown within InvocationCallback", t);
                            } finally {
                                // If this is a response then, it must be closed by the runtime as defined in
                                // InvocationCallback.completed()
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
            final ResultExtractor<T> extractor, final ExecutorService executorService) {
        if (closed.get()) {
            return CompletableFuture.failedFuture(LogMessages.LOGGER.clientIsClosed());
        }
        try {
            final HttpClient client = httpClient(request.getUri());
            final HttpRequest httpRequest = createRequest(request, client);
            final HttpResponse.BodyHandler<InputStream> handler;
            if (buffered) {
                handler = HttpResponse.BodyHandlers.buffering(HttpResponse.BodyHandlers.ofInputStream(),
                        configuration.responseBufferSize());
            } else {
                handler = HttpResponse.BodyHandlers.ofInputStream();
            }
            final BiFunction<HttpResponse<InputStream>, Throwable, HttpResponse<InputStream>> responseHandler = (response,
                    error) -> {
                if (error != null) {
                    Throwable cause = error;
                    if (cause instanceof CompletionException && cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    // Close the results before we throw the error
                    if (response != null) {
                        try {
                            response.body().close();
                        } catch (Exception ignore) {
                        }
                    }
                    throw LogMessages.LOGGER.unableToInvokeRequest(cause, cause.toString());
                }
                return response;
            };
            return client.sendAsync(httpRequest, handler)
                    .handle(responseHandler)
                    .thenApply((response) -> extractor.extractResult(createResponse(client, request, response)));
        } catch (Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }

    /**
     * Returns the {@link HttpClient} to use for the given request URI.
     * <p>
     * Implementations may return the same client for all URIs (standard mode with connection pooling)
     * or create a new client per URI (per-request mode for per-URI SSL contexts).
     * </p>
     *
     * @param uri the target URI for the request
     * @return the {@code HttpClient} to use for this request
     */
    abstract HttpClient httpClient(URI uri);

    /**
     * Closes or performs cleanup for the specified HTTP client.
     * <p>
     * Implementations should handle two cases:
     * </p>
     * <ul>
     * <li>When {@code client} is non-null: close this specific client (per-request cleanup)</li>
     * <li>When {@code client} is null: close all clients and perform final cleanup (engine shutdown)</li>
     * </ul>
     *
     * @param client the client to close, or {@code null} to close all clients during engine shutdown
     */
    abstract void closeClient(HttpClient client);

    private HttpRequest createRequest(final ClientInvocation clientInvocation, final HttpClient client) throws IOException {
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(clientInvocation.getUri());

        final long readTimeout = configuration.readTimeout(TimeUnit.MILLISECONDS);
        if (readTimeout > 1) {
            requestBuilder.timeout(Duration.ofMillis(readTimeout));
        }
        final String method = clientInvocation.getMethod();
        if ("GET".equals(method)) {
            // The TCK seems to require a -1 for the content-length of GET requests. We'll just satisfy that here as
            // it's likely not a big deal.
            requestBuilder.method(method, HttpRequest.BodyPublishers.fromPublisher(HttpRequest.BodyPublishers.noBody()));
        } else {
            if (clientInvocation.getEntity() != null) {
                if (NO_BODY_REQUEST_METHODS.contains(method.toUpperCase(Locale.ROOT))) {
                    throw LogMessages.LOGGER.bodyNotAllowed(method);
                }
                final ClientEntityOutputStream out = new ClientEntityOutputStream();
                if (clientInvocation.isChunked()) {
                    // Chunked transfer encoding only works with HTTP/1.1
                    requestBuilder.version(HttpClient.Version.HTTP_1_1)
                            .header("Transfer-Encoding", "chunked");
                }
                // Checkstyle chokes on this and throws an NPE. Once this is fixed we should prefer the try-with-resources
                //try (out) {
                //noinspection TryFinallyCanBeTryWithResources
                try {
                    clientInvocation.getDelegatingOutputStream().setDelegate(out);
                    clientInvocation.writeRequestBody(clientInvocation.getEntityStream());
                } finally {
                    out.close();
                }
                // If this is not a chunked request, we want to wrap the publisher with the real size. Otherwise, we
                // need the size of -1 so the content-length is not defined in the header
                requestBuilder.method(method, out.toPublisher(!clientInvocation.isChunked()));
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
        // Set the HTTP version if defined
        if (configuration.configuration().hasProperty(HttpClientConfigurationOptions.HTTP_VERSION)) {
            final Object value = configuration.configuration().getProperty(HttpClientConfigurationOptions.HTTP_VERSION);
            if (value instanceof HttpClient.Version) {
                requestBuilder.version((HttpClient.Version) value);
            } else {
                LogMessages.LOGGER.invalidVersion(value, client.version());
            }
        } else {
            ConfigurationFactory.getInstance().getConfiguration()
                    .getOptionalValue(HttpClientConfigurationOptions.HTTP_VERSION, HttpClient.Version.class)
                    .ifPresent(requestBuilder::version);
        }
        return requestBuilder.build();
    }

    private ClientResponse createResponse(final HttpClient client, final ClientInvocation clientInvocation,
            final HttpResponse<InputStream> response) {
        final ClientResponse clientResponse = new ClientResponse(clientInvocation.getClientConfiguration(),
                clientInvocation.getTracingLogger()) {

            @Override
            public void releaseConnection(final boolean consumeInputStream) throws IOException {
                try {
                    final InputStream in = getInputStream();
                    if (in != null) {
                        // Drain the request if requested up to 64KB
                        if (consumeInputStream) {
                            in.readNBytes(64 * 1024);
                        }
                        in.close();
                    }
                } finally {
                    closeClient(client);
                }
            }

            @Override
            protected void setInputStream(final InputStream is) {
                //super.setInputStream(is);
                this.is = is;
                resetEntity();
            }

            @Override
            protected InputStream getInputStream() {
                if (is != null || isClosed()) {
                    return is;
                }
                if (NO_BODY_RESPONSE_METHODS.contains(response.request().method().toUpperCase(Locale.ROOT))) {
                    return null;
                }
                return this.is = response.body();
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
                headers.add(name, value);
            }
        });
        return headers;
    }

    private static class ClientEntityOutputStream extends EntityOutputStream {

        HttpRequest.BodyPublisher toPublisher(final boolean wrapWithSize) throws IOException {
            final EntityInputStream entityInputStream = toInputStream();
            final HttpRequest.BodyPublisher delegate = HttpRequest.BodyPublishers.ofInputStream(() -> entityInputStream);
            final long len = wrapWithSize ? entityInputStream.size() : -1;
            // The HttpRequest.BodyPublishers.fromPublisher(delegate, len) does not allow for -1 of the len, while the TCK
            // in some cases requires -1 to be returned. This is a simple workaround.
            return wrapWithSize ? new DelegateBodyPublisher(delegate, len) : delegate;
        }
    }

    private static class DelegateBodyPublisher implements HttpRequest.BodyPublisher {
        private final HttpRequest.BodyPublisher delegate;
        private final long len;

        private DelegateBodyPublisher(final HttpRequest.BodyPublisher delegate, final long len) {
            this.delegate = delegate;
            this.len = len;
        }

        @Override
        public long contentLength() {
            return len;
        }

        @Override
        public void subscribe(final Flow.Subscriber<? super ByteBuffer> subscriber) {
            delegate.subscribe(subscriber);
        }
    }
}
