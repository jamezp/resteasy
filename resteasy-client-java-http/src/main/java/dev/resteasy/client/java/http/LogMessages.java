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

package dev.resteasy.client.java.http;

import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.http.HttpClient;
import java.util.function.Supplier;

import jakarta.ws.rs.ProcessingException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "RESTEASY-CLIENT")
public interface LogMessages extends BasicLogger {

    LogMessages LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), LogMessages.class,
            LogMessages.class.getPackage().getName());

    @LogMessage(level = Level.DEBUG)
    @Message(id = 100, value = "Ignoring exception thrown within InvocationCallback")
    void exceptionIgnored(@Cause Throwable ex);

    @LogMessage(level = Level.DEBUG)
    @Message(id = 101, value = "Client send processing failure.")
    void clientSendProcessingFailure(@Cause Throwable cause);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 1187, value = "Closing a %s instance for you. Please close clients yourself.")
    void closingForYou(Class<?> clazz);

    @LogMessage(level = Level.WARN)
    @Message(id = 1300, value = "Could not determine the HttpClient.Version from %s. Defaulting to %s.")
    void invalidVersion(Object found, HttpClient.Version version);

    @LogMessage(level = Level.WARN)
    @Message(id = 1302, value = "Using a HostnameVerifier is not supported for the java.net.http.HttpClient. " +
            "Falling back to the Apache HTTP Client. Note that HTTP/2 support will not be available with the client.")
    void hostnameVerifierFound();

    @Message(id = 110, value = "Client is closed.")
    ProcessingException clientIsClosed();

    @Message(id = 1140, value = "Stream is closed")
    String streamIsClosed();

    @Message(id = 1155, value = "Unable to invoke request: %s")
    ProcessingException unableToInvokeRequest(@Cause Throwable cause, String msg);

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
