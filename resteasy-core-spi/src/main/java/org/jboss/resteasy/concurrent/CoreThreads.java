/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2023 Red Hat, Inc., and individual contributors
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

package org.jboss.resteasy.concurrent;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class CoreThreads {

    static int getCoreThreads(final String name) {
        final var value = System.getProperty(name);
        if (value == null) {
            return Math.max(5, Runtime.getRuntime().availableProcessors());
        }
        return Integer.parseInt(value);
    }
}
