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

package dev.resteasy.client.java.net;

import java.net.http.HttpClient;
import java.util.function.Supplier;

import org.jboss.resteasy.spi.config.Options;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class HttpClientOptions<T> extends Options<T> {

    /**
     * An option for generating a custom {@linkplain HttpClient.Builder builder} when using the {@link HttpClient} as
     * the backing client for the {@linkplain jakarta.ws.rs.client.Client REST client}.
     *
     * @since 6.3
     */
    public static final HttpClientOptions<HttpClient.Builder> HTTP_CLIENT_BUILDER = new HttpClientOptions<>(
            "dev.resteasy.client.builder",
            HttpClient.Builder.class,
            HttpClient::newBuilder);

    /**
     * An option for defining the HTTP version when using the {@link HttpClient} as the backing client for the
     * {@linkplain jakarta.ws.rs.client.Client REST client}.
     * <p>
     * The default is HTTP_2.
     * </p>
     *
     * @since 6.3
     */
    public static final HttpClientOptions<HttpClient.Version> HTTP_CLIENT_VERSION = new HttpClientOptions<>(
            "dev.resteasy.client.http.version",
            HttpClient.Version.class,
            () -> HttpClient.Version.HTTP_2);

    private HttpClientOptions(final String key, final Class<T> name, final Supplier<T> dftValue) {
        super(key, name, dftValue);
    }
}
