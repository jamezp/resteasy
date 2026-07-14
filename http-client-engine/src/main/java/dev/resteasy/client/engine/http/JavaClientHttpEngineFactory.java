/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.client.engine.http;

import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import jakarta.annotation.Priority;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.api.ClientBuilderConfiguration;
import org.jboss.resteasy.client.jaxrs.engine.ClientHttpEngineFactory;
import org.jboss.resteasy.client.jaxrs.engines.AsyncClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.spi.ClientConfigProvider;
import org.jboss.resteasy.spi.PriorityServiceLoader;

/**
 * Factory for creating HTTP client engines backed by the JDK's {@link HttpClient}.
 * <p>
 * This factory is discovered via {@link java.util.ServiceLoader} and automatically creates the appropriate
 * engine implementation based on the client configuration:
 * </p>
 * <ul>
 * <li><strong>Standard mode:</strong> When no {@link ClientConfigProvider} is registered, creates a
 * {@link JavaClientHttpEngine} that reuses a single {@code HttpClient} instance for all requests,
 * providing connection pooling and optimal performance.</li>
 * <li><strong>Per-request mode:</strong> When a {@link ClientConfigProvider} is found, creates a
 * {@link PerRequestJavaClientHttpEngine} that builds a new {@code HttpClient} for each request using
 * per-URI SSL contexts. This mode has a performance penalty but enables per-URI SSL configuration.</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Priority(500)
public class JavaClientHttpEngineFactory implements ClientHttpEngineFactory {

    @Override
    public ClientHttpEngine httpClientEngine(final ClientBuilderConfiguration configuration) {
        return ClientHttpEngineFactory.super.httpClientEngine(configuration);
    }

    @Override
    public AsyncClientHttpEngine asyncHttpClientEngine(final ClientBuilderConfiguration configuration) {
        final ClientConfigProvider configProvider = findClientConfigProvider();
        if (configProvider == null) {
            SSLParameters sslParameters = null;
            if (!configuration.sniHostNames().isEmpty()) {
                sslParameters = new SSLParameters();
                sslParameters.setServerNames(configuration.sniHostNames()
                        .stream()
                        .map(SNIHostName::new)
                        .collect(Collectors.toList()));
            }
            return new JavaClientHttpEngine(
                    createHttpClient(configuration, configuration.sslContext(), sslParameters), configuration);
        }
        return new PerRequestJavaClientHttpEngine(uri -> {
            SSLParameters sslParameters = null;
            // Use the configuration provider to attempt to get the SSL context for the hostname
            final URI targetUri;
            if (uri.getPort() < 0) {
                targetUri = URI.create(String.format("%s://%s", uri.getScheme(), uri.getHost()));
            } else {
                targetUri = URI.create(String.format("%s://%s:%d", uri.getScheme(), uri.getHost(), uri.getPort()));
            }
            SSLContext sslContext = configProvider.getSSLContext(targetUri);
            if (sslContext == null) {
                // Use the default SSL context
                sslContext = configuration.sslContext();
                if (!configuration.sniHostNames().isEmpty()) {
                    sslParameters = new SSLParameters();
                    sslParameters.setServerNames(configuration.sniHostNames()
                            .stream()
                            .map(SNIHostName::new)
                            .collect(Collectors.toList()));
                }
            }
            return createHttpClient(configuration, sslContext, sslParameters);
        }, configuration);
    }

    private HttpClient createHttpClient(final ClientBuilderConfiguration configuration,
            final SSLContext sslContext,
            final SSLParameters sslParameters) {
        final HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                // Use NORMAL to follow redirects while preventing HTTPS -> HTTP downgrades
                .followRedirects(configuration.isFollowRedirects() ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER);

        // Set the client builder if applicable
        configuration.executorService().ifPresent(clientBuilder::executor);

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

        if (sslContext != null) {
            if (sslParameters != null) {
                clientBuilder.sslParameters(sslParameters);
            }
            clientBuilder.sslContext(sslContext);
        }
        return clientBuilder.build();
    }

    private static ClientConfigProvider findClientConfigProvider() {
        return PriorityServiceLoader.load(ClientConfigProvider.class, getClassLoader()).first()
                .orElse(null);
    }

    private static ClassLoader getClassLoader() {
        ClassLoader result = Thread.currentThread().getContextClassLoader();
        if (result == null) {
            result = ClientConfigProvider.class.getClassLoader();
        }
        return result;
    }

    private record SslConfig(SSLContext sslContext, SSLParameters sslParameters) {
    }
}
