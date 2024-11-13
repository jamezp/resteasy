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

package dev.resteasy.client.java.net;

import java.io.OutputStream;
import java.util.function.Supplier;

import jakarta.ws.rs.ProcessingException;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.Message.Format;
import org.jboss.logging.annotations.MessageBundle;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "RESTEASY-CLIENT")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(Messages.class);

    @Message(id = 110, value = "Client is closed.")
    String clientIsClosed();

    @Message(id = 1140, value = "Stream is closed")
    String streamIsClosed();

    @Message(id = 1155, value = "Unable to invoke request: {0}", format = Format.MESSAGE_FORMAT)
    String unableToInvokeRequest(String msg);

    @Message(id = 1192, value = "Stream has not been closed. Cannot create input stream from %s.")
    IllegalStateException streamNotClosed(OutputStream out);

    @Message(id = 1193, value = "The stream has already been exported.")
    Supplier<IllegalStateException> alreadyExported();

    @Message(id = 1195, value = "Request method %s cannot have a body.")
    ProcessingException bodyNotAllowed(String method);

    @Message(id = 1196, value = "The hostname verifier cannot be set on the HttpClientEngine")
    IllegalStateException hostnameVerifierSet();

    @Message(id = 1197, value = "Failed to get the response content. The connection may have been closed.")
    IllegalStateException noContentFound();
}
