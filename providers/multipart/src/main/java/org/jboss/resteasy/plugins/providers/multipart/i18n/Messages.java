package org.jboss.resteasy.plugins.providers.multipart.i18n;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;

import org.apache.james.mime4j.stream.Field;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.Message.Format;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.resteasy.plugins.providers.multipart.AbstractMultipartWriter;
import org.jboss.resteasy.plugins.providers.multipart.MultipartOutput;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LoggingSupport;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright Aug 25, 2015
 */
@MessageBundle(projectCode = "RESTEASY")
public interface Messages {
    Messages MESSAGES = LoggingSupport.getBundle(MethodHandles.lookup(), Messages.class);

    @Message(id = 7500, value = "Could find no Content-Disposition header within part")
    String couldFindNoContentDispositionHeader();

    @Message(id = 7505, value = "Could not parse Content-Disposition for MultipartFormData: {0}", format = Format.MESSAGE_FORMAT)
    String couldNotParseContentDisposition(Field field);

    @Message(id = 7510, value = "This DataSource represents an incoming xop message part. Getting an OutputStream on it is not allowed.")
    String dataSourceRepresentsXopMessagePart();

    @Message(id = 7515, value = "Exception while extracting attachment with cid = %s from xop message to a byte[].")
    String exceptionWhileExtractionAttachment(String cid);

    @Message(id = 7520, value = "Had to write out multipartoutput = {0} with writer = {1} but this writer can only handle {2}", format = Format.MESSAGE_FORMAT)
    String hadToWriteMultipartOutput(MultipartOutput multipartOutput, AbstractMultipartWriter writer, Class<?> clazz);

    @Message(id = 7525, value = "No attachment with cid = {0} (Content-ID = {1}) found in xop message.", format = Format.MESSAGE_FORMAT)
    String noAttachmentFound(String cid, String contentId);

    @Message(id = 7530, value = "This provider and this method are not meant for stand alone usage.")
    String notMeantForStandaloneUsage();

    @Message(id = 7535, value = "Reader = {0} received genericType = {1}, but it is not instance of {2}", format = Format.MESSAGE_FORMAT)
    String receivedGenericType(MessageBodyReader<?> reader, Type genericType, Class<?> clazz);

    @Message(id = 7540, value = "SwaRefs are not supported in xop creation.")
    String swaRefsNotSupported();

    @Message(id = 7545, value = "Unable to find a MessageBodyReader for media type: {0} and class type {1}", format = Format.MESSAGE_FORMAT)
    String unableToFindMessageBodyReader(MediaType mediaType, String type);

    @Message(id = 7550, value = "Unable to get boundary for multipart")
    String unableToGetBoundary();

    @Message(id = 7555, value = "java.net.URLDecoder does not support UTF-8 encoding")
    String urlDecoderDoesNotSupportUtf8();

    @Message(id = 7560, value = "java.net.URLEncoder does not support UTF-8 encoding")
    String urlEncoderDoesNotSupportUtf8();

    @Message(id = 7565, value = "Parameter %s is a required parameter and cannot be set to null.")
    String nullParameter(String name);

    @Message(id = 7566, value = "Cannot invoke EntityPart.getContent more than once.")
    IllegalStateException getContentAlreadyInvoked();
}
