package org.jboss.resteasy.plugins.providers.jsonb.i18n;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.Message.Format;
import org.jboss.logging.annotations.MessageBundle;

/**
 */
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(Messages.class);

    @Message(id = 8200, value = "JSON Binding deserialization error: {0}", format = Format.MESSAGE_FORMAT)
    String jsonBDeserializationError(String rootCauseMsg);

    @Message(id = 8205, value = "JSON Binding serialization error {0}", format = Format.MESSAGE_FORMAT)
    String jsonBSerializationError(String element);
}
