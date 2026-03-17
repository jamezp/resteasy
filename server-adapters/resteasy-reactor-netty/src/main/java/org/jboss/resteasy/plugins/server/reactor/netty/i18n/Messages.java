package org.jboss.resteasy.plugins.server.reactor.netty.i18n;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright Sep 1, 2015
 * @deprecated use the new dependencies
 */
@Deprecated(forRemoval = true, since = "6.2.13.Final")
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(Messages.class);

    @Message(id = 22500, value = "Already committed")
    String alreadyCommitted();

    @Message(id = 22505, value = "Already suspended")
    String alreadySuspended();

    @Message(id = 22510, value = "Response write aborted abruptly")
    String responseWriteAborted();
}