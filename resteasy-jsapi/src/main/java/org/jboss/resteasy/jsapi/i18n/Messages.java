package org.jboss.resteasy.jsapi.i18n;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.Message.Format;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.resteasy.core.ResourceMethodInvoker;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright Aug 27, 2015
 */
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(MethodHandles.lookup(), Messages.class);

    @Message(id = 11500, value = "Impossible to generate JSAPI for subresource returned by method {0}.{1} since return type is not a static JAXRS resource type", format = Format.MESSAGE_FORMAT)
    String impossibleToGenerateJsapi(String className, String methodName);

    @Message(id = 11505, value = " Invoker: %s")
    String invoker(ResourceMethodInvoker invoker);

    @Message(id = 11510, value = "JSAPIServlet loaded")
    String jsapiServletLoaded();

    @Message(id = 11515, value = "Loading JSAPI Servlet")
    String loadingJSAPIServlet();

    @Message(id = 11520, value = "Overriding @Consumes annotation in favour of application/x-www-form-urlencoded due to the presence of @FormParam")
    String overridingConsumes();

    @Message(id = 11525, value = "Path: %s")
    String path(String uri);

    @Message(id = 11530, value = "Query %s")
    String query(String query);

    @Message(id = 11535, value = "REST.apiURL = '%s';")
    String restApiUrl(String uri);

    @Message(id = 11545, value = "Serving %s")
    String serving(String pathinfo);

    @Message(id = 11550, value = "// start REST API")
    String startJaxRsApi();

    @Message(id = 11555, value = "// start RESTEasy client API")
    String startResteasyClient();

    @Message(id = 11560, value = "There are no Resteasy deployments initialized yet to scan from.  Either set the load-on-startup on each Resteasy servlet, or, if in an EE environment like JBoss or Wildfly, you'll have to do an invocation on each of your REST services to get the servlet loaded.")
    String thereAreNoResteasyDeployments();
}
