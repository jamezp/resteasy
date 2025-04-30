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

import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.api.ClientBuilderConfiguration;
import org.jboss.resteasy.client.jaxrs.engine.ClientHttpEngineFactory;
import org.jboss.resteasy.client.jaxrs.engines.AsyncClientHttpEngine;
import org.jboss.resteasy.concurrent.ContextualExecutors;
import org.kohsuke.MetaInfServices;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MetaInfServices
public class JavaHttpClientEngineFactory implements ClientHttpEngineFactory {
    @Override
    public ClientHttpEngine httpClientEngine(final ClientBuilderConfiguration configuration) {
        return ClientHttpEngineFactory.super.httpClientEngine(configuration);
    }

    @Override
    public AsyncClientHttpEngine asyncHttpClientEngine(final ClientBuilderConfiguration configuration) {
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
        return new JavaHttpClientEngine(clientBuilder.build(), configuration);
    }
}
