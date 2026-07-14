/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.client.engine.http._private;

import java.lang.invoke.MethodHandles;
import java.net.http.HttpClient;

import jakarta.ws.rs.ProcessingException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "RESTEASY-HTTP-CLIENT")
public interface LogMessages extends BasicLogger {

    LogMessages LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), LogMessages.class,
            LogMessages.class.getPackage().getName());

    @LogMessage(level = Level.WARN)
    @Message(id = 110, value = "Could not determine the HttpClient.Version from %s. Defaulting to %s.")
    void invalidVersion(Object found, HttpClient.Version version);

    @Message(id = 120, value = "Client is closed.")
    ProcessingException clientIsClosed();

    @Message(id = 130, value = "Unable to invoke request: %s")
    ProcessingException unableToInvokeRequest(@Cause Throwable cause, String msg);

    @Message(id = 140, value = "Request method %s cannot have a body.")
    ProcessingException bodyNotAllowed(String method);
}
