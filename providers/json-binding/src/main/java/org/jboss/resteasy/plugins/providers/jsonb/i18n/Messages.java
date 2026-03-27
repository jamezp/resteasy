package org.jboss.resteasy.plugins.providers.jsonb.i18n;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.Message.Format;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LoggingSupport;

/**
 */
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = LoggingSupport.getBundle(MethodHandles.lookup(), Messages.class);

    @Message(id = 8200, value = "JSON Binding deserialization error: {0}", format = Format.MESSAGE_FORMAT)
    String jsonBDeserializationError(String rootCauseMsg);

    @Message(id = 8205, value = "JSON Binding serialization error {0}", format = Format.MESSAGE_FORMAT)
    String jsonBSerializationError(String element);
}
