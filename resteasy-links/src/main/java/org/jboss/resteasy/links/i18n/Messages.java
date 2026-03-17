package org.jboss.resteasy.links.i18n;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.Message.Format;
import org.jboss.logging.annotations.MessageBundle;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright Aug 28, 2015
 */
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(Messages.class);

    @Message(id = 12000, value = "Cannot guess collection type for service discovery")
    String cannotGuessCollectionType();

    @Message(id = 12005, value = "Cannot guess resource type for service discovery")
    String cannotGuessResourceType();

    @Message(id = 12010, value = "Cannot guess type for Response")
    String cannotGuessType();

    @Message(id = 12015, value = "Could not instantiate ELProvider class %s")
    String couldNotInstantiateELProviderClass(String className);

    @Message(id = 12020, value = "Discovery failed for method {0}.{1}: {2}", format = Format.MESSAGE_FORMAT)
    String discoveryFailedForMethod(String className, String methodName, String s);

    @Message(id = 12025, value = "Failed to evaluate EL expression: %s")
    String failedToEvaluateELExpression(String expression);

    @Message(id = 12030, value = "Failed to find bean property %s")
    String failedToFindBeanProperty(String property);

    @Message(id = 12035, value = "Failed to inject links in %s")
    String failedToInjectLinks(Object entity);

    @Message(id = 12040, value = "Failed to instantiate ELProvider: %s")
    String failedToInstantiateELProvider(String className);

    @Message(id = 12045, value = "Failed to read field %s")
    String failedToReadField(String field);

    @Message(id = 12050, value = "Failed to read property %s")
    String failedToReadProperty(String property);

    @Message(id = 12055, value = "Failed to read property from method %s")
    String failedToReadPropertyFromMethod(String property);

    @Message(id = 12060, value = "Not enough URI parameters: expecting {0} but only found {1}", format = Format.MESSAGE_FORMAT)
    String notEnoughtUriParameters(int expected, int actual);

    @Message(id = 12065, value = "Failed to access/reuse user-created service discovery in %s")
    String failedToReuseServiceDiscovery(Object entity);
}
