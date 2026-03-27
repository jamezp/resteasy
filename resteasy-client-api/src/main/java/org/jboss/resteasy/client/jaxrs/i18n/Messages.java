package org.jboss.resteasy.client.jaxrs.i18n;

import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.function.Supplier;

import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.Message.Format;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.annotations.Param;
import org.jboss.logging.annotations.Signature;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LoggingSupport;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright Aug 26, 2015
 */
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = LoggingSupport.getBundle(MethodHandles.lookup(), Messages.class);

    @Message(id = 4500, value = "You can only set one of LinkHeaderParam.rel() and LinkHeaderParam.title() for on {0}.{1}", format = Format.MESSAGE_FORMAT)
    String canOnlySetOneLinkHeaderParam(String className, String methodName);

    @Message(id = 4505, value = "Cannot set a form parameter if entity body already set")
    String cannotSetFormParameter();

    @Message(id = 4510, value = "Client is closed.")
    String clientIsClosed();

    @Message(id = 4515, value = "Client Proxy for : %s")
    String clientProxyFor(String className);

    @Message(id = 4520, value = "Could not create a URL for {0} in {1}.{2}", format = Format.MESSAGE_FORMAT)
    String couldNotCreateURL(String uri, String className, String methodName);

    @Message(id = 4525, value = "Marking file '%s' to be deleted, as it could not be deleted while processing request:")
    String couldNotDeleteFile(String path);

    @Message(id = 4530, value = "Could not find a method for: %s")
    String couldNotFindMethod(Method method);

    @Message(id = 4535, value = "Could not process method %s")
    String couldNotProcessMethod(Method method);

    @Message(id = 4540, value = "%s does not specify the type parameter T of GenericType<T>")
    String doesNotSpecifyTypeParameter(TypeVariable<?> var);

    @Message(id = 4545, value = "The entity was already read, and it was of type %s")
    String entityAlreadyRead(Class<?> clazz);

    @Message(id = 4550, value = "failed on registering class: %s")
    String failedOnRegisteringClass(String className);

    @Message(id = 4555, value = "Failed to buffer aborted response")
    String failedToBufferAbortedResponse();

    @Message(id = 4560, value = "Failed to buffer aborted response. Could not find writer for content-type {0} type: {1}", format = Format.MESSAGE_FORMAT)
    String failedToBufferAbortedResponseNoWriter(MediaType mediaType, String className);

    @Message(id = 4565, value = "A GET request cannot have a body.")
    String getRequestCannotHaveBody();

    @Message(id = 4575, value = "Input stream was empty, there is no entity")
    String inputStreamWasEmpty();

    @Message(id = 4580, value = "link was null")
    String linkWasNull();

    @Message(id = 4585, value = "method was null")
    String methodWasNull();

    @Message(id = 4590, value = "You must define a @Consumes type on your client method or interface, or supply a default")
    String mustDefineConsumesType();

    @Message(id = 4595, value = "You must set either LinkHeaderParam.rel() or LinkHeaderParam.title() for on {0}.{1}", format = Format.MESSAGE_FORMAT)
    String mustSetLinkHeaderParam(String className, String methodName);

    @Message(id = 4600, value = "You must use at least one, but no more than one http method annotation on: %s")
    String mustUseExactlyOneHttpMethod(String methodName);

    @Message(id = 4605, value = "name was null")
    String nameWasNull();

    @Message(id = 4610, value = "No type information to extract entity with.  You use other getEntity() methods")
    String noTypeInformation();

    @Message(id = 4615, value = "parameters was null")
    String parametersWasNull();

    @Message(id = 4620, value = "path was null")
    String pathWasNull();

    @Message(id = 4623, value = "Please consider updating the version of Apache HttpClient being used.  Version 4.3.6+ is recommended.")
    String pleaseConsiderUnpdating();

    @Message(id = 4625, value = "proxyInterface was null")
    String proxyInterfaceWasNull();

    @Message(id = 4630, value = "resource was null")
    String resourceWasNull();

    @Message(id = 4635, value = "Resteasy Client Proxy for : %s")
    String resteasyClientProxyFor(String className);

    @Message(id = 4640, value = "Stream is closed")
    String streamIsClosed();

    @Message(id = 4645, value = "templateValues entry was null")
    String templateValuesEntryWasNull();

    @Message(id = 4650, value = "templateValues was null")
    String templateValuesWasNull();

    @Message(id = 4652, value = "Unable to create new instance of %s")
    String unableToInstantiate(Class<?> clazz);

    @Message(id = 4655, value = "Unable to invoke request: {0}", format = Format.MESSAGE_FORMAT)
    String unableToInvokeRequest(String msg);

    @Message(id = 4660, value = "uriBuilder was null")
    String uriBuilderWasNull();

    @Message(id = 4665, value = "uri was null")
    String uriWasNull();

    @Message(id = 4670, value = "value was null")
    String valueWasNull();

    @Message(id = 4680, value = "Could not close http response")
    String couldNotCloseHttpResponse();

    @Message(id = 4685, value = "Unable to set http proxy")
    String unableToSetHttpProxy();

    @Message(id = 4690, value = "Parameter annotated with %s cannot be null")
    String nullParameter(String annotation);

    @Message(id = 4691, value = "Unable to load ClientConfigProvider configuration because uri is null")
    String unableToLoadClientConfigProviderConfiguration();

    @Message(id = 4692, value = "Stream has not been closed. Cannot create input stream from %s.")
    IllegalStateException streamNotClosed(OutputStream out);

    @Message(id = 4693, value = "The stream has already been exported.")
    Supplier<IllegalStateException> alreadyExported();

    @Message(id = 4694, value = "No content type found in response. Cannot extract the response value.")
    @Signature(messageIndex = 1, value = { Response.class, String.class })
    ResponseProcessingException noContentTypeFound(@Param Response response);

    @Message(id = 4695, value = "Expected type %s, but found type %s for the configuration.")
    IllegalStateException invalidClientBuilderConfiguration(Class<?> found, Class<?> expected);
}
