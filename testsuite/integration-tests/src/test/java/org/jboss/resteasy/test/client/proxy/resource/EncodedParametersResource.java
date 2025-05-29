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

package org.jboss.resteasy.test.client.proxy.resource;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class EncodedParametersResource implements EncodedParameters {
    @Override
    public String path(final String value) {
        return value;
    }

    @Override
    public String pathEncoded(final String value) {
        return value;
    }

    @Override
    public String query(final String value) {
        return value;
    }

    @Override
    public String queryEncoded(final String value) {
        return value;
    }

    @Override
    public String matrix(final String value) {
        return value;
    }

    @Override
    public String matrixEncoded(final String value) {
        return value;
    }

    @Override
    public String form(final String value) {
        return value;
    }

    @Override
    public String formEncoded(final String value) {
        return value;
    }
}
