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

package org.jboss.resteasy.test.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Indicates the test requires an HTTP server. The HTTP server returns 200 with an empty body for {@code HEAD} and
 * {@code GET} requests. For a {@code POST} request, the request body is echoed back in the response with a status
 * code of 200. Any other request method will result in a 405.
 *
 * <p>
 * A special path of {@code /chunked} is registered for testing chunked transfer encoding. For {@code POST} requests
 * to this path, the server validates both the {@code Transfer-Encoding} header and request body content. If the
 * header equals {@code "chunked"} (case-insensitive) and the request body equals {@code "file entity"}, the server
 * returns {@code "ok"} with status 200. If either condition fails, {@code "not ok"} is returned with status 400.
 * </p>
 * <p>
 * If any other HTTP request method is sent to the {@code /chunked} path, status 405 is returned.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(TestHttpServerExtension.class)
public @interface RequiresHttpServer {
}
