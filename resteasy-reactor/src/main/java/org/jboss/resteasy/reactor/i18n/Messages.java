package org.jboss.resteasy.reactor.i18n;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(MethodHandles.lookup(), Messages.class);

    @Message(id = 20501, value = "Expected ClientInvocationBuilder, not: %s")
    String expectedClientInvocationBuilder(String className);
}
