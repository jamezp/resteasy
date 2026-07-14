/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.client.engine.http;

import java.net.URI;
import java.net.http.HttpClient;

import org.jboss.resteasy.client.jaxrs.api.ClientBuilderConfiguration;

import dev.resteasy.client.engine.http._private.LogMessages;

/**
 * Standard HTTP client engine implementation that reuses a single {@link HttpClient} instance for all requests.
 * <p>
 * This implementation provides optimal performance through connection pooling and keep-alive. The underlying
 * {@code HttpClient} is shared across all requests and is only closed when {@link #close()} is called.
 * </p>
 * <p>
 * This is the default engine used when no {@link org.jboss.resteasy.client.jaxrs.spi.ClientConfigProvider}
 * is registered. For per-URI SSL context support, see {@link PerRequestJavaClientHttpEngine}.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class JavaClientHttpEngine extends AbstractJavaClientHttpEngine {
    private final HttpClient httpClient;

    JavaClientHttpEngine(final HttpClient httpClient, final ClientBuilderConfiguration configuration) {
        super(configuration);
        this.httpClient = httpClient;
    }

    @Override
    HttpClient httpClient(final URI uri) {
        return httpClient;
    }

    @Override
    void closeClient(final HttpClient client) {
        // Do not close clients per request. We only close the client when the ClientHttpEngine.close() is invoked.
        if (client == null) {
            if (httpClient instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) httpClient).close();
                } catch (final Exception e) {
                    LogMessages.LOGGER.debugf(e, "Failed to close HTTP Client: %s", httpClient);
                }
            }
        }
    }
}
