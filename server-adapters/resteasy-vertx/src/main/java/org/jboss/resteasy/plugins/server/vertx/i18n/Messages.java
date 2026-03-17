package org.jboss.resteasy.plugins.server.vertx.i18n;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright Sep 1, 2015
 * @deprecated use new dependencies
 */
@Deprecated(forRemoval = true, since = "6.2.13.Final")
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(Messages.class);

    @Message(id = 19500, value = "Already committed")
    String alreadyCommitted();

    @Message(id = 19505, value = "Already suspended")
    String alreadySuspended();

    @Message(id = 19510, value = "Chunk size must be at least 1")
    String chunkSizeMustBeAtLeastOne();

    @Message(id = 19520, value = "response is committed")
    String responseIsCommitted();

    @Message(id = 19525, value = "Unexpected")
    String unexpected();
}
