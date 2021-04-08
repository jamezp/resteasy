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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Priority;
import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.core.Encoder;
import org.jboss.resteasy.core.XmlEncoder;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Provider
@ConstrainedTo(RuntimeType.SERVER)
// TODO (jrp) is this the right priority?
@Priority(Priorities.ENTITY_CODER)
public class EncoderFeature implements DynamicFeature {

    // TODO (jrp) should these be in the ResteasyContextParameters?
    private static final String ENCODE_MEDIA_TYPES = "resteasy.encode.ignored.media.types";
    // TODO (jrp) "ignore" or "skip"?
    private static final String SKIP_ENCODING = "resteasy.encode.skip";
    private static final List<MediaType> DEFAULT_IGNORED_MEDIA_TYPES = Arrays.asList(
            MediaType.APPLICATION_ATOM_XML_TYPE,
            MediaType.APPLICATION_SVG_XML_TYPE,
            MediaType.APPLICATION_XHTML_XML_TYPE,
            MediaType.APPLICATION_XML_TYPE,
            MediaType.TEXT_HTML_TYPE,
            MediaType.TEXT_XML_TYPE
    );

    @Override
    public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
        final Collection<MediaType> mediaTypes = getMediaTypes(resourceInfo);
        final ResteasyConfiguration config = ResteasyProviderFactory.getContextData(ResteasyConfiguration.class);
        if (config != null) {
            String skip = config.getParameter(SKIP_ENCODING);
            if (skip == null) {
                skip = config.getInitParameter(SKIP_ENCODING);
            }
            if (Boolean.parseBoolean(skip)) {
                return;
            }
        }
        final Collection<MediaType> ignoredMediaTypes = ignoredMediaTypes(config);

        for (MediaType mediaType : mediaTypes) {
            if (ignoredMediaTypes.contains(mediaType)) {
                return;
            }
        }
        final Encoder encoder = new XmlEncoder();
        context.register(new EncoderParamConverterProvider(encoder));
        /*if (addInterceptor(resourceInfo)) {
            context.register(new EncoderReaderInterceptor(encoder), 1);
        }*/
    }

    private static Collection<MediaType> getMediaTypes(final ResourceInfo resourceInfo) {
        // First attempt to get the media types from the method
        final Method method = resourceInfo.getResourceMethod();
        Consumes consumes = method.getAnnotation(Consumes.class);
        if (consumes == null) {
            consumes = resourceInfo.getResourceClass().getAnnotation(Consumes.class);
        }
        return consumes == null ? Collections.singleton(MediaType.WILDCARD_TYPE) : toMediaTypes(consumes.value());
    }

    private static Collection<MediaType> toMediaTypes(final String[] values) {
        final Collection<MediaType> result = new ArrayList<>();
        for (String value : values) {
            result.add(MediaType.valueOf(value));
        }
        return result;
    }

    private static Collection<MediaType> ignoredMediaTypes(final ResteasyConfiguration configuration) {
        if (configuration != null) {
            String value = configuration.getParameter(ENCODE_MEDIA_TYPES);
            if (value == null) {
                value = configuration.getInitParameter(ENCODE_MEDIA_TYPES);
            }
            if (value != null) {
                final Set<MediaType> result = new HashSet<>();
                final String[] mediaTypes = value.split(",");
                for (String mediaType : mediaTypes) {
                    result.add(MediaType.valueOf(mediaType));
                }
                return Collections.unmodifiableSet(result);
            }
        }
        return DEFAULT_IGNORED_MEDIA_TYPES;
    }

    private static boolean addInterceptor(final ResourceInfo resourceInfo) {
        // First attempt to get the media types from the method
        final Method method = resourceInfo.getResourceMethod();
        // TODO (jrp) this is not efficient and really could be wrong.
        if (method.isAnnotationPresent(FormParam.class)) {
            return false;
        }
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(FormParam.class)) {
                return false;
            }
        }
        return !fieldHasAnnotation(resourceInfo.getResourceClass(), FormParam.class);
    }

    private static boolean fieldHasAnnotation(final Class<?> type, final Class<? extends Annotation> annotation) {
        if (type == null) {
            return false;
        }
        for (Field field : type.getDeclaredFields()) {
            if (field.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return fieldHasAnnotation(type.getSuperclass(), annotation);
    }
}
