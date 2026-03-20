package org.jboss.resteasy.test.common;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

/**
 * Tiny test HTTP server providing a target for testing the RESTEasy client.
 */
class TestHttpServer implements AutoCloseable {

    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private final AtomicBoolean started = new AtomicBoolean(false);

    private volatile Undertow server;
    private volatile String baseUri;

    TestHttpServer() {
    }

    /**
     * The base URI for the server.
     *
     * @return The host and port the server is listening on.
     */
    String baseUri() {
        return baseUri;
    }

    /**
     * Starts the server.
     */
    TestHttpServer start() {
        if (started.compareAndSet(false, true)) {
            final PathHandler pathHandler = new PathHandler(new DefaultHttpHandler());
            // for ChunkedTransferEncodingUnitTest
            pathHandler.addPrefixPath("/chunked", new HttpHandler() {
                @Override
                public void handleRequest(final HttpServerExchange exchange) {
                    try {
                        final String response;
                        final int status;

                        if (Methods.POST.equals(exchange.getRequestMethod())) {
                            // Redispatch on a blocking handler
                            if (exchange.isInIoThread()) {
                                exchange.dispatch(new BlockingHandler(this));
                                return;
                            }
                            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            exchange.getInputStream().transferTo(buffer);

                            final String transferEncoding = exchange.getRequestHeaders()
                                    .getFirst(Headers.TRANSFER_ENCODING);
                            if ("chunked".equalsIgnoreCase(transferEncoding)
                                    && Arrays.equals(buffer.toByteArray(), "file entity".getBytes())) {
                                response = "ok";
                                status = 200;
                            } else {
                                response = "not ok";
                                status = 400;
                            }
                        } else {
                            response = "Method Not Allowed";
                            status = 405;
                        }

                        exchange.setStatusCode(status);
                        exchange.getResponseSender().send(response);
                    } catch (Exception e) {
                        exchange.setStatusCode(500);
                        exchange.getResponseSender().send("Error: " + e.getMessage());
                    }
                }
            });
            this.server = Undertow.builder()
                    .addHttpListener(0, "127.0.0.1")
                    .setHandler(pathHandler)
                    .build();

            server.start();

            // Get the actual bound address
            final Undertow.ListenerInfo listenerInfo = server.getListenerInfo().get(0);
            final InetSocketAddress bindAddress = (InetSocketAddress) listenerInfo.getAddress();
            this.baseUri = String.format("%s://%s:%d", listenerInfo.getProtcol(), bindAddress.getHostString(),
                    bindAddress.getPort());
        }
        return this;
    }

    @Override
    public void close() throws Exception {
        started.set(false);
        final Undertow server = this.server;
        if (server != null) {
            server.stop();
        }
    }

    private static class DefaultHttpHandler implements HttpHandler {

        @Override
        public void handleRequest(final HttpServerExchange exchange) throws Exception {
            final byte[] response;
            final int status;
            final HttpString method = exchange.getRequestMethod();

            if (Methods.HEAD.equals(method)) {
                response = EMPTY_BYTE_ARRAY;
                status = 200;
            } else if (Methods.GET.equals(method)) {
                response = EMPTY_BYTE_ARRAY;
                status = 200;
            } else if (Methods.POST.equals(method)) {
                // Redispatch on a blocking handler
                if (exchange.isInIoThread()) {
                    exchange.dispatch(new BlockingHandler(this));
                    return;
                }
                final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                exchange.getInputStream().transferTo(buffer);
                response = buffer.toByteArray();
                status = 200;
            } else {
                response = "Method Not Allowed".getBytes(StandardCharsets.UTF_8);
                status = 405;
            }

            exchange.setStatusCode(status);
            exchange.getResponseSender().send(ByteBuffer.wrap(response));
        }
    }
}
