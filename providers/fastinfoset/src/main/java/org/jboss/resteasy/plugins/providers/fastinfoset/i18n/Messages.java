package org.jboss.resteasy.plugins.providers.fastinfoset.i18n;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LoggingSupport;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright Aug 24, 2015
 */
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = LoggingSupport.getBundle(MethodHandles.lookup(), Messages.class);

    @Message(id = 5500, value = "Expecting a StreamSource")
    String expectingStreamSource();
}
