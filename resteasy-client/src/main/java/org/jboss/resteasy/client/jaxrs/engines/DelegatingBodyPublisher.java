/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
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

package org.jboss.resteasy.client.jaxrs.engines;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class DelegatingBodyPublisher implements HttpRequest.BodyPublisher {
    private final HttpRequest.BodyPublisher delegate;
    private final long contentLength;

    DelegatingBodyPublisher(final HttpRequest.BodyPublisher delegate) {
        this(delegate, -1);
    }

    DelegatingBodyPublisher(final HttpRequest.BodyPublisher delegate, final long contentLength) {
        this.delegate = delegate;
        this.contentLength = contentLength;
    }

    static DelegatingBodyPublisher of(final HttpRequest.BodyPublisher delegate) {
        return new DelegatingBodyPublisher(delegate);
    }

    static DelegatingBodyPublisher of(final HttpRequest.BodyPublisher delegate, final long contentLength) {
        return new DelegatingBodyPublisher(delegate, contentLength);
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public void subscribe(final Flow.Subscriber<? super ByteBuffer> subscriber) {
        delegate.subscribe(subscriber);
    }
}
