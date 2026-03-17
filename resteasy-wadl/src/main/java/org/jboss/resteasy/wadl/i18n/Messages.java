package org.jboss.resteasy.wadl.i18n;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.Message.Format;
import org.jboss.logging.annotations.MessageBundle;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright January 6, 2016
 */
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(MethodHandles.lookup(), Messages.class);

    @Message(id = 19000, value = "Impossible to generate WADL for subresource returned by method {0}.{1} since return type is not a static JAXRS resource type", format = Format.MESSAGE_FORMAT)
    String impossibleToGenerateWADL(String className, String methodName);

    @Message(id = 19005, value = "Loading ResteasyWadlServlet")
    String loadingResteasyWadlServlet();

    @Message(id = 19010, value = "There are no Resteasy deployments initialized yet to scan from. Either set the load-on-startup on each Resteasy servlet, or, if in an EE environment like JBoss or Wildfly, you'll have to do an invocation on each of your REST services to get the servlet loaded.")
    String noResteasyDeployments();

    @Message(id = 19015, value = "Overriding @Consumes annotation in favour of application/x-www-form-urlencoded due to the presence of @FormParam")
    String overridingConsumesAnnotation();

    @Message(id = 19020, value = "Path: %s")
    String path(String key);

    @Message(id = 19025, value = "Query %s")
    String query(String query);

    @Message(id = 19030, value = "ResteasyWadlServlet loaded")
    String resteasyWadlServletLoaded();

    @Message(id = 19035, value = "Serving %s")
    String servingPathInfo(String pathInfo);

    @Message(id = 19036, value = "Error while processing WADL")
    String cantProcessWadl();
}
