package org.jboss.resteasy.plugins.server.sun.http.i18n;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright Sep 1, 2015
 */
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(MethodHandles.lookup(), Messages.class);

    @Message(id = 17500, value = "[Embedded Container Start]")
    String embeddedContainerStart();

    @Message(id = 17505, value = "[Embedded Container Stop]")
    String embeddedContainerStop();

    @Message(id = 17510, value = "Error parsing request")
    String errorParsingRequest();

    @Message(id = 17515, value = "WTF!")
    String wtf();
}
