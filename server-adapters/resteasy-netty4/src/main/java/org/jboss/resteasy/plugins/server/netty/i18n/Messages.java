package org.jboss.resteasy.plugins.server.netty.i18n;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright Sep 1, 2015
 *
 * @deprecated use the new dependencies
 */
@Deprecated(forRemoval = true, since = "6.2.13.Final")
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(Messages.class);

    @Message(id = 18500, value = "Already committed")
    String alreadyCommitted();

    @Message(id = 18505, value = "Already suspended")
    String alreadySuspended();

    @Message(id = 18510, value = "Chunk size must be at least 1")
    String chunkSizeMustBeAtLeastOne();

    @Message(id = 18512, value = "Exception caught by handler")
    String exceptionCaught();

    @Message(id = 18515, value = "Failed to parse request.")
    String failedToParseRequest();

    @Message(id = 18520, value = "response is committed")
    String responseIsCommitted();

    @Message(id = 18525, value = "Unexpected")
    String unexpected();
}
