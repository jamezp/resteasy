/**
 * HTTP client engine implementation using the Java's built-in {@code java.net.http.HttpClient}.
 * <p>
 * This module provides a zero-dependency HTTP client engine for RESTEasy with native HTTP/2 support.
 * The engine is automatically discovered via {@link java.util.ServiceLoader} when this module is present
 * on the module path or classpath.
 * </p>
 * <p>
 * Key features:
 * </p>
 * <ul>
 * <li>HTTP/1.1 and HTTP/2 with automatic protocol negotiation</li>
 * <li>Asynchronous request execution with {@link java.util.concurrent.CompletableFuture}</li>
 * <li>Zero external dependencies (uses JDK built-in HTTP client)</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @since 7.0
 */
module dev.resteasy.client.engine.http {
    // JDK modules
    requires transitive java.net.http;

    // Jakarta modules
    requires jakarta.annotation;

    // Third-party dependencies
    requires org.jboss.logging;
    requires static org.jboss.logging.annotations;

    // RESTEasy modules
    requires org.jboss.resteasy.client;
    requires org.jboss.resteasy.client.api;
    requires org.jboss.resteasy.core;
    requires org.jboss.resteasy.spi;

    // Exports
    exports dev.resteasy.client.engine.http.config;

    // Open the package to the SPI module since we don't export any packages
    opens dev.resteasy.client.engine.http to org.jboss.resteasy.spi;

    // Provides
    provides org.jboss.resteasy.client.jaxrs.engine.ClientHttpEngineFactory with
            dev.resteasy.client.engine.http.JavaClientHttpEngineFactory;
}