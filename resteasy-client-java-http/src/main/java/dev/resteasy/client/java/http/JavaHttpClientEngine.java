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

package dev.resteasy.client.java.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner;
import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.api.ClientBuilderConfiguration;
import org.jboss.resteasy.client.jaxrs.engines.AsyncClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.concurrent.ContextualExecutors;
import org.jboss.resteasy.spi.EntityOutputStream;
import org.jboss.resteasy.spi.ResourceCleaner;
import org.jboss.resteasy.util.CaseInsensitiveMap;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class JavaHttpClientEngine implements AsyncClientHttpEngine {
    private static final Collection<String> NO_BODY_REQUEST_METHODS = List.of(
            "CONNECT",
            "GET",
            "HEAD");
    private static final Collection<String> NO_BODY_RESPONSE_METHODS = List.of(
            "HEAD"
    //"TRACE" the spec seems to allow TRACE methods to have a response body
    );
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final HttpClient httpClient;
    private final ClientBuilderConfiguration configuration;

    public JavaHttpClientEngine(final HttpClient httpClient, final ClientBuilderConfiguration configuration) {
        this.httpClient = httpClient;
        this.configuration = configuration;
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
                                LogMessages.LOGGER.exceptionIgnored(t);
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
            final HttpRequest httpRequest = createRequest(request);
            final HttpResponse.BodyHandler<InputStream> handler;
            if (buffered) {
                handler = HttpResponse.BodyHandlers.buffering(HttpResponse.BodyHandlers.ofInputStream(), Integer.MAX_VALUE);
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
                    try {
                        response.body().close();
                    } catch (Exception ignore) {
                    }
                    throw LogMessages.LOGGER.unableToInvokeRequest(cause, cause.toString());
                }
                return response;
            };
            return httpClient.sendAsync(httpRequest, handler)
                    .handle(responseHandler)
                    .thenApply((response) -> extractor.extractResult(createResponse(request, response)));
        } catch (Throwable t) {
            return CompletableFuture.failedFuture(t);
        }
    }

    @Override
    public SSLContext getSslContext() {
        return configuration.sslContext();
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return null;
    }

    @Override
    public Response invoke(final Invocation request) {
        if (closed.get()) {
            throw LogMessages.LOGGER.clientIsClosed();
        }
        final ClientInvocation clientInvocation = (ClientInvocation) request;
        try {
            final HttpResponse<InputStream> response = httpClient.send(createRequest(clientInvocation),
                    HttpResponse.BodyHandlers.ofInputStream());
            return createResponse(clientInvocation, response);
        } catch (Exception e) {
            LogMessages.LOGGER.clientSendProcessingFailure(e);
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
        // TODO (jrp) we may want to use a MR-JAR so we can close this in Java 21
        if (closed.compareAndSet(false, true)) {
            // TODO (jrp) what do we do here?
            /*
             * AutoCloseable closeable;
             * while ((closeable = closeables.poll()) != null) {
             * try {
             * closeable.close();
             * } catch (Exception e) {
             * LogMessages.LOGGER.tracef(e, "Failed closing %s", closeable);
             * }
             * }
             */
        }
    }

    private HttpRequest createRequest(final ClientInvocation clientInvocation) throws IOException {
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
                final ClientEntityOutputStream out = new ClientEntityOutputStream(
                        () -> "resteasy-" + clientInvocation + "-" + method);
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
        return requestBuilder.build();

        // configuration.connectionIdleTime();
        // configuration.readTimeout();
        // configuration.scheduledExecutorService();
    }

    private ClientResponse createResponse(final ClientInvocation clientInvocation,
            final HttpResponse<InputStream> response) {
        final ClientResponse clientResponse = new ClientResponse(clientInvocation.getClientConfiguration(),
                clientInvocation.getTracingLogger()) {

            @Override
            public void releaseConnection(final boolean consumeInputStream) throws IOException {
                final InputStream in = getInputStream();
                if (in != null) {
                    in.close();
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

    private Executor resolveExecutor(final ClientInvocation clientInvocation) {
        if (clientInvocation.asyncInvocationExecutor() != null) {
            return clientInvocation.asyncInvocationExecutor();
        }
        return configuration.executorService().orElse(ContextualExecutors.threadPool());
    }

    private HttpClient createClient() {

        final HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .executor(configuration.executorService().orElse(ContextualExecutors.threadPool()))
                // TODO (jrp) should we use NORMAL or NEVER. Tests seem to expect NEVER.
                .followRedirects(configuration.isFollowRedirects() ? HttpClient.Redirect.ALWAYS : HttpClient.Redirect.NEVER);

        final long connectionTimeout = configuration.connectionTimeout(TimeUnit.MILLISECONDS);
        if (connectionTimeout > 0L) {
            clientBuilder.connectTimeout(Duration.ofMillis(connectionTimeout));
        }
        final String proxyHostname = configuration.defaultProxyHostname();
        if (proxyHostname != null) {
            final ProxySelector proxySelector = ProxySelector.of(
                    InetSocketAddress.createUnresolved(configuration.defaultProxyHostname(), configuration.defaultProxyPort()));
            clientBuilder.proxy(proxySelector);
        }

        if (configuration.isCookieManagementEnabled()) {
            clientBuilder.cookieHandler(new CookieManager());
        }

        if (configuration.sslContext() != null) {
            if (!configuration.sniHostNames().isEmpty()) {
                final SSLParameters sslParameters = new SSLParameters();
                sslParameters.setServerNames(configuration.sniHostNames()
                        .stream()
                        .map(SNIHostName::new)
                        .collect(Collectors.toList()));
                clientBuilder.sslParameters(sslParameters);
            }
            // TODO (jrp) how do we deal with the ClientConfigProvider.getSSLContext(uri)
            clientBuilder.sslContext(configuration.sslContext());
        }

        // configuration.connectionIdleTime();
        // configuration.readTimeout();
        // configuration.scheduledExecutorService();
        return clientBuilder.build();
    }

    private static CaseInsensitiveMap<String> extractHeaders(final HttpResponse<?> response) {
        final CaseInsensitiveMap<String> headers = new CaseInsensitiveMap<>();
        response.headers().map().forEach((name, values) -> {
            for (String value : values) {
                // TODO (jrp) do we need to do this?
                //headers.add(ResponseHeaders.lowerToDefault(name), value);
                headers.add(name.toLowerCase(Locale.ROOT), value);
            }
        });
        return headers;
    }

    private static class ClientEntityOutputStream extends EntityOutputStream {

        /**
         * Creates a new entity stream with the maximum in memory threshold and a file prefix to be used if the stream needs
         * to be written to a file due to the threshold.
         *
         * @param filePrefix the file prefix if a file is created
         */
        ClientEntityOutputStream(final Supplier<String> filePrefix) {
            super(filePrefix);
        }

        HttpRequest.BodyPublisher toPublisher(final boolean wrapWithSize) throws IOException {
            if (!isClosed()) {
                throw LogMessages.LOGGER.streamNotClosed(this);
            }
            checkExported(LogMessages.LOGGER.alreadyExported());
            final HttpRequest.BodyPublisher delegate;
            final long len;
            // TODO (jrp) use a real lock
            synchronized (lock) {
                final Path file = getFile();
                final Supplier<InputStream> stream;
                if (file != null) {
                    stream = () -> new CleanableFileInputStream(file);
                    len = wrapWithSize ? Files.size(file) : -1;
                } else {
                    final byte[] bytes = getAndClearMemory();
                    len = wrapWithSize ? bytes.length : -1;
                    stream = () -> new ByteArrayInputStream(bytes);
                }
                delegate = HttpRequest.BodyPublishers.ofInputStream(stream);
            }
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

    // TODO (jrp) do we even want this?
    @SuppressWarnings("NullableProblems")
    private static class CleanableFileInputStream extends InputStream {
        private final Path file;
        private final AtomicBoolean closed;
        private final Lock lock;
        private volatile Cleaner.Cleanable cleanable;
        private volatile InputStream delegate;

        private CleanableFileInputStream(final Path file) {
            this.file = file;
            closed = new AtomicBoolean(false);
            lock = new ReentrantLock();
        }

        @Override
        public int read() throws IOException {
            checkClosed();
            return getDelegate().read();
        }

        @Override
        public int read(final byte[] b) throws IOException {
            checkClosed();
            return getDelegate().read(b);
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            checkClosed();
            return getDelegate().read(b, off, len);
        }

        @Override
        public byte[] readAllBytes() throws IOException {
            checkClosed();
            return getDelegate().readAllBytes();
        }

        @Override
        public byte[] readNBytes(final int len) throws IOException {
            checkClosed();
            return getDelegate().readNBytes(len);
        }

        @Override
        public int readNBytes(final byte[] b, final int off, final int len) throws IOException {
            checkClosed();
            return getDelegate().readNBytes(b, off, len);
        }

        @Override
        public long skip(final long n) throws IOException {
            checkClosed();
            return getDelegate().skip(n);
        }

        @Override
        public int available() throws IOException {
            checkClosed();
            return getDelegate().available();
        }

        @Override
        public void close() throws IOException {
            if (closed.compareAndSet(false, true)) {
                lock.lock();
                try {
                    try {
                        if (delegate != null) {
                            delegate.close();
                        }
                    } finally {
                        if (cleanable != null) {
                            cleanable.clean();
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        @Override
        public void mark(final int readlimit) {
            if (!closed.get()) {
                getDelegate().mark(readlimit);
            }
        }

        @Override
        public void reset() throws IOException {
            checkClosed();
            getDelegate().reset();
        }

        @Override
        public boolean markSupported() {
            return !closed.get() && getDelegate().markSupported();
        }

        @Override
        public long transferTo(final OutputStream out) throws IOException {
            checkClosed();
            return getDelegate().transferTo(out);
        }

        private void checkClosed() throws IOException {
            if (closed.get()) {
                throw new IOException(LogMessages.LOGGER.streamIsClosed());
            }
        }

        private InputStream getDelegate() {
            lock.lock();
            try {
                if (delegate == null) {
                    if (Files.notExists(file)) {
                        throw LogMessages.LOGGER.noContentFound();
                    }
                    try {
                        delegate = Files.newInputStream(file);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    cleanable = ResourceCleaner.register(delegate, () -> {

                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException e) {
                            org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages.LOGGER.debugf(e, "Failed to delete file %s",
                                    file);
                        }
                    });
                }
                return delegate;
            } finally {
                lock.unlock();
            }
        }
    }
}
