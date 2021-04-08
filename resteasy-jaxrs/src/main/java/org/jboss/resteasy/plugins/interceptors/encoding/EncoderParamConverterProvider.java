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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.jboss.resteasy.core.Encoder;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class EncoderParamConverterProvider implements ParamConverterProvider {
    private final Encoder encoder;

    public EncoderParamConverterProvider(final Encoder encoder) {
        this.encoder = encoder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
        if (rawType.isAssignableFrom(String.class) && hasSupportedAnnotation(annotations)) {
            // TODO (jrp) we should do something for CharSequence
            return (ParamConverter<T>) new EncoderParamConverter(encoder);
        }
        return null;
    }

    private static boolean hasSupportedAnnotation(final Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (isSupportedAnnotation(annotation)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSupportedAnnotation(final Annotation annotation) {
        if (annotation instanceof PathParam) {
            return true;
        } else if (annotation instanceof HeaderParam) {
            // TODO (jrp) what do we need to do here? What types should we names should we not convert?
            return false;
        } else if (annotation instanceof QueryParam) {
            return true;
        } else if (annotation instanceof MatrixParam) {
            return true;
        } else {
            return annotation instanceof CookieParam;
        }
    }
}
