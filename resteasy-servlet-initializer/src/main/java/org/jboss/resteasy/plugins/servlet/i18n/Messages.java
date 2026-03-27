package org.jboss.resteasy.plugins.servlet.i18n;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LoggingSupport;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright Aug 29, 2015
 */
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = LoggingSupport.getBundle(MethodHandles.lookup(), Messages.class);

    @Message(id = 12500, value = "Default Application class not implemented yet")
    String defaultApplicationNotImplemented();
}
