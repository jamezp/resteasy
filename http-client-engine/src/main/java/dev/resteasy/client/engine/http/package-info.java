/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * HTTP client engine implementation using the JDK's built-in {@link java.net.http.HttpClient}.
 * <p>
 * This package provides a zero-dependency HTTP client engine for RESTEasy with native HTTP/2 support.
 * The engine is automatically discovered via {@link java.util.ServiceLoader} when the
 * {@code resteasy-http-client-engine} dependency is present.
 * </p>
 *
 * <h2>Engine Selection</h2>
 * <p>
 * The {@link dev.resteasy.client.engine.http.JavaClientHttpEngineFactory} automatically selects the
 * appropriate engine implementation based on client configuration:
 * </p>
 * <ul>
 * <li><strong>Standard mode ({@link dev.resteasy.client.engine.http.JavaClientHttpEngine}):</strong>
 * Used by default. Reuses a single {@code HttpClient} instance for optimal performance through
 * connection pooling and keep-alive.</li>
 * <li><strong>Per-request mode ({@link dev.resteasy.client.engine.http.PerRequestJavaClientHttpEngine}):</strong>
 * Used when a {@link org.jboss.resteasy.client.jaxrs.spi.ClientConfigProvider} is registered.
 * Creates a new {@code HttpClient} per request to support per-URI SSL contexts. This mode has
 * significant performance overhead.</li>
 * </ul>
 *
 * <h2>Features</h2>
 * <ul>
 * <li>HTTP/1.1 and HTTP/2 with automatic protocol negotiation</li>
 * <li>Asynchronous request execution with {@link java.util.concurrent.CompletableFuture}</li>
 * <li>Connection pooling and keep-alive (standard mode)</li>
 * <li>Chunked transfer encoding (HTTP/1.1 only)</li>
 * <li>Zero external dependencies</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * <ul>
 * <li>Custom {@link javax.net.ssl.HostnameVerifier} is not supported</li>
 * <li>Per-URI SSL contexts require per-request mode with performance penalty</li>
 * </ul>
 *
 * @since 7.0
 */
package dev.resteasy.client.engine.http;
