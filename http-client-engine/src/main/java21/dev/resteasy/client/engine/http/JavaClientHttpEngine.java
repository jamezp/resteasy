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
 * Java 21+ variant of {@link JavaClientHttpEngine} that uses {@link HttpClient#shutdownNow()} for immediate cleanup.
 * <p>
 * On Java 21+, {@link HttpClient#close()} performs an orderly shutdown that waits for all pending operations
 * (including unconsumed response streams) to complete, which can block for a long time if callers have not
 * properly closed their {@link jakarta.ws.rs.core.Response} objects. Since the Jakarta REST
 * {@link jakarta.ws.rs.client.Client#close()} contract is to release all resources immediately,
 * {@code shutdownNow()} is the correct match.
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
            try {
                httpClient.shutdownNow();
            } catch (final Exception e) {
                LogMessages.LOGGER.debugf(e, "Failed to close HTTP Client: %s", httpClient);
            }
        }
    }
}
