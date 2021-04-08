/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
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

package org.jboss.resteasy.plugins.interceptors.encoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.annotation.Priority;
import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.jboss.resteasy.core.Encoder;
import org.jboss.resteasy.core.XmlEncoder;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ConstrainedTo(RuntimeType.SERVER)
@Provider
@Priority(Priorities.ENTITY_CODER)
public class EncoderReaderInterceptor implements ReaderInterceptor {
    private final Encoder encoder;

    // TODO (jrp) remove this, what we really need is a way to discover the encoders
    public EncoderReaderInterceptor() {
        this(new XmlEncoder());
    }

    public EncoderReaderInterceptor(final Encoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
        final MediaType mediaType = context.getMediaType();
        final byte[] buffer = new byte[256];
        int len;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final InputStream in = context.getInputStream();
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        final Charset charset;
        if (mediaType == null) {
            charset = StandardCharsets.UTF_8;
        } else {
            final String c = mediaType.getParameters().get("charset");
            if (c == null) {
                charset = StandardCharsets.UTF_8;
            } else {
                charset = Charset.forName(c);
            }
        }
        context.setInputStream(new ByteArrayInputStream(encoder.encode(new String(out.toByteArray(), charset)).getBytes(charset)));
        return context.proceed();
    }
}
