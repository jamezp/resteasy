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

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class HostVerificationTrustManager extends X509ExtendedTrustManager {
    private final X509TrustManager delegate;
    private final HostnameVerifier verifier;

    HostVerificationTrustManager(final X509TrustManager delegate, final HostnameVerifier verifier) {
        this.delegate = delegate;
        this.verifier = verifier;
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType, final Socket socket)
            throws CertificateException {
        if (socket instanceof SSLSocket) {
            verifier.verify(socket.getInetAddress().getHostName(), ((SSLSocket) socket).getSession());
        }
        delegate.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType, final Socket socket)
            throws CertificateException {
        if (socket instanceof SSLSocket) {
            verifier.verify(socket.getInetAddress().getHostName(), ((SSLSocket) socket).getSession());
        }
        delegate.checkServerTrusted(chain, authType);
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType, final SSLEngine engine)
            throws CertificateException {
        verifier.verify(engine.getPeerHost(), engine.getSession());
        delegate.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType, final SSLEngine engine)
            throws CertificateException {
        verifier.verify(engine.getPeerHost(), engine.getSession());
        delegate.checkServerTrusted(chain, authType);
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        delegate.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        delegate.checkServerTrusted(chain, authType);

    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }
}
