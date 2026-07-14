/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.client.engine.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import org.jboss.resteasy.client.jaxrs.api.ClientBuilderConfiguration;

import dev.resteasy.client.engine.http._private.LogMessages;

/**
 * Per-request HTTP client engine that creates a new {@link HttpClient} for each request.
 * <p>
 * This implementation is used when a {@link org.jboss.resteasy.client.jaxrs.spi.ClientConfigProvider} is
 * registered, enabling per-URI SSL context configuration. Each request gets its own {@code HttpClient}
 * instance configured with the appropriate SSL context for the target URI.
 * </p>
 * <p>
 * <strong>Performance Note:</strong> Creating a new {@code HttpClient} per request has significant overhead:
 * </p>
 * <ul>
 * <li>No connection pooling between requests</li>
 * <li>Initialization overhead per request (~10-50ms)</li>
 * <li>Increased memory allocation</li>
 * </ul>
 * <p>
 * Created {@code HttpClient} instances are tracked and closed when the engine is closed or when individual
 * requests complete. For standard usage without per-URI SSL contexts, see {@link JavaClientHttpEngine}.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class PerRequestJavaClientHttpEngine extends AbstractJavaClientHttpEngine {
    private final Queue<AutoCloseable> closeables = new ConcurrentLinkedQueue<>();
    private final Function<URI, HttpClient> httpClientFunction;

    PerRequestJavaClientHttpEngine(final Function<URI, HttpClient> httpClientFunction,
            final ClientBuilderConfiguration configuration) {
        super(configuration);
        this.httpClientFunction = httpClientFunction;
    }

    @Override
    HttpClient httpClient(final URI uri) {
        final HttpClient client = httpClientFunction.apply(uri);
        if (client instanceof AutoCloseable) {
            closeables.add((AutoCloseable) client);
        }
        return client;
    }

    @Override
    void closeClient(final HttpClient client) {
        final Iterator<AutoCloseable> iterator = closeables.iterator();
        while (iterator.hasNext()) {
            final AutoCloseable closeable = iterator.next();
            if (client != null && !client.equals(closeable)) {
                continue;
            }
            try {
                iterator.remove();
                closeable.close();
                if (client != null) {
                    break;
                }
            } catch (final Exception e) {
                LogMessages.LOGGER.debugf(e, "Exception while closing %s", closeable);
            }
        }
    }
}
