/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.client.engine.http;

import java.net.URI;
import java.net.http.HttpClient;

import org.jboss.resteasy.client.jaxrs.api.ClientBuilderConfiguration;

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
        // On Java 17-20, HttpClient has no close/shutdown methods — cleanup is handled by GC.
        // On Java 21+, the multi-release JAR variant of this class calls shutdownNow().
    }
}
