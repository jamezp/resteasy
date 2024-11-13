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

package org.jboss.resteasy.client.jaxrs.spi;

import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.net.ssl.SSLContext;

import org.jboss.resteasy.spi.PriorityServiceLoader;

/**
 * Used to resolve an {@link SSLContext} for clients. If the default SSL context is not {@code null}, then there is an
 * attempt to resolve the SSL context from the {@link ClientConfigProvider#getSSLContext(URI) ClientConfigProvider},
 * if not {@code null}, based on the {@linkplain URI uri}.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class SslContextResolver {
    private final SSLContext defaultContext;
    private final ClientConfigProvider configProvider;

    /**
     * Creates a new SSL context resolver
     *
     * @param defaultContext the default SSL context, can be {@code null}
     * @param configProvider the configuration provider, can be {@code null}
     */
    public SslContextResolver(final SSLContext defaultContext, final ClientConfigProvider configProvider) {
        this.defaultContext = defaultContext;
        this.configProvider = configProvider;
    }

    /**
     * Creates a new SSL context resolver with the default SSL context. The {@link ClientConfigProvider} is resolved
     * via a {@link java.util.ServiceLoader}.
     *
     * @param defaultContext the default SSL context or {@code null} if there is not one and the {@link ClientConfigProvider}
     *                       should be used to resolve the SSL context
     *
     * @return a new SSL context resolver
     */
    public static SslContextResolver of(final SSLContext defaultContext) {
        final ClientConfigProvider configProvider;
        if (System.getSecurityManager() == null) {
            configProvider = PriorityServiceLoader.load(ClientConfigProvider.class).first().orElse(null);
        } else {
            configProvider = AccessController.doPrivileged(
                    (PrivilegedAction<ClientConfigProvider>) () -> PriorityServiceLoader.load(ClientConfigProvider.class)
                            .first()
                            .orElse(null));
        }
        return new SslContextResolver(defaultContext, configProvider);
    }

    /**
     * Creates a new SSL context resolver with the default SSL context.
     *
     * @param defaultContext the default SSL context or {@code null} if there is not one and the {@link ClientConfigProvider}
     *                       should be used to resolve the SSL context
     * @param configProvider the configuration provider or {@code null} if there is no configuration provider
     *
     * @return a new SSL context resolver
     */
    public static SslContextResolver of(final SSLContext defaultContext, final ClientConfigProvider configProvider) {
        return new SslContextResolver(defaultContext, configProvider);
    }

    /**
     * Resolves a {@link SSLContext}. If the default SSL context is {@code null} and the {@link ClientConfigProvider} is
     * <em>not</em> {@code null}, the {@link ClientConfigProvider#getSSLContext(URI) ClientConfigProvider} is used
     * to resolve the SSL context.
     * <p>
     * If both the default SSL context and client config provider are {@code null}, {@code null} is returned.
     * </p>
     *
     * @param uri the URI used to resolve the {@link SSLContext} from the {@link ClientConfigProvider}
     *
     * @return the SSL context or {@code null} if one was not defined
     */
    public SSLContext resolve(final URI uri) {
        if (defaultContext != null) {
            return defaultContext;
        }
        if (configProvider != null) {
            return configProvider.getSSLContext(uri);
        }
        return null;
    }
}
