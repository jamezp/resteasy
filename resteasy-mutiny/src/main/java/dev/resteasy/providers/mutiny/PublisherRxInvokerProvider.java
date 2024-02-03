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

package dev.resteasy.providers.mutiny;

import java.util.concurrent.ExecutorService;

import jakarta.ws.rs.client.RxInvokerProvider;
import jakarta.ws.rs.client.SyncInvoker;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class PublisherRxInvokerProvider implements RxInvokerProvider<PublisherRxInvoker> {
    @Override
    public boolean isProviderFor(final Class<?> clazz) {
        return PublisherRxInvoker.class.isAssignableFrom(clazz);
    }

    @Override
    public PublisherRxInvoker getRxInvoker(final SyncInvoker syncInvoker, final ExecutorService executorService) {
        return new PublisherRxInvoker(syncInvoker, executorService);
    }
}
