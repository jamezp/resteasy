package org.jboss.resteasy.core;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.WriterInterceptor;

import org.jboss.resteasy.core.interception.jaxrs.AbstractWriterInterceptorContext;
import org.jboss.resteasy.core.interception.jaxrs.ContainerResponseContextImpl;
import org.jboss.resteasy.core.interception.jaxrs.ResponseContainerRequestContext;
import org.jboss.resteasy.core.interception.jaxrs.ServerWriterInterceptorContext;
import org.jboss.resteasy.core.registry.SegmentNode;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.spi.AsyncOutputStream;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.tracing.RESTEasyTracingLogger;
import org.jboss.resteasy.util.CommitHeaderAsyncOutputStream;
import org.jboss.resteasy.util.CommitHeaderOutputStream;
import org.jboss.resteasy.util.CommitHeaderOutputStream.CommitCallback;
import org.jboss.resteasy.util.HttpHeaderNames;
import org.jboss.resteasy.util.MediaTypeHelper;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServerResponseWriter {
    @FunctionalInterface
    public interface RunnableWithIOException {
        void run(Consumer<Throwable> onComplete) throws IOException;
    }

    private static Produces WILDCARD_PRODUCES = new Produces() {

        @Override
        public Class<? extends Annotation> annotationType() {
            return Produces.class;
        }

        @Override
        public String[] value() {
            return new String[] { "*", "*" };
        }
    };

    public static void writeNomapResponse(BuiltResponse jaxrsResponse, final HttpRequest request, final HttpResponse response,
            final ResteasyProviderFactory providerFactory, Consumer<Throwable> onComplete) throws IOException {
        writeNomapResponse(jaxrsResponse, request, response, providerFactory, onComplete, true);
    }

    @Deprecated
    public static void writeNomapResponse(BuiltResponse jaxrsResponse, final HttpRequest request, final HttpResponse response,
            final ResteasyProviderFactory providerFactory) throws IOException {
        writeNomapResponse(jaxrsResponse, request, response, providerFactory, t -> {
        }, true);
    }

    @Deprecated
    public static void writeNomapResponse(BuiltResponse jaxrsResponse, final HttpRequest request, final HttpResponse response,
            final ResteasyProviderFactory providerFactory, boolean sendHeaders) throws IOException {
        writeNomapResponse(jaxrsResponse, request, response, providerFactory, t -> {
        }, sendHeaders);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void writeNomapResponse(BuiltResponse jaxrsResponse, final HttpRequest request, final HttpResponse response,
            final ResteasyProviderFactory providerFactory, Consumer<Throwable> onComplete, boolean sendHeaders)
            throws IOException {
        ResourceMethodInvoker method = (ResourceMethodInvoker) request.getAttribute(ResourceMethodInvoker.class.getName());

        // do this even if we're not sending the headers, because this sets the content type in the response,
        // which is used by marshalling, and NPEs otherwise
        setResponseMediaType(jaxrsResponse, request, response, providerFactory, method);

        executeFilters(jaxrsResponse, request, response, providerFactory, method, onComplete, (onWriteComplete) -> {
            Object entity = jaxrsResponse.isClosed() ? null : jaxrsResponse.getEntity();

            //[RESTEASY-1627] check on response.getOutputStream() to avoid resteasy-netty4 trying building a chunked response body for HEAD requests
            if (entity == null || response.getOutputStream() == null) {
                response.setStatus(jaxrsResponse.getStatus());
                commitHeaders(jaxrsResponse, response);
                onWriteComplete.accept(null);
                return;
            }

            Class type = jaxrsResponse.getEntityClass();
            Type generic = jaxrsResponse.getGenericType();
            Annotation[] annotations = jaxrsResponse.getAnnotations();
            MediaType mt = jaxrsResponse.getMediaType();
            // A filter could set the entity without setting the media type. While the java doc for Response.setEntity(Object)
            // states "The existing entity annotations and media type are preserved.", we need the media type for the
            // writer. We'll go ahead and set a default.
            if (mt == null) {
                mt = getDefaultContentType(request, jaxrsResponse, providerFactory, method);
                if (mt != null) {
                    jaxrsResponse.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, mt);
                }
            }
            MessageBodyWriter writer = providerFactory.getMessageBodyWriter(
                    type, generic, annotations, mt);
            if (writer != null)
                LogMessages.LOGGER.debugf("MessageBodyWriter: %s", writer.getClass().getName());

            if (writer == null) {
                response.setStatus(jaxrsResponse.getStatus()); //set the status to the response status anyway
                onWriteComplete.accept(new NoMessageBodyWriterFoundFailure(type, mt));
                return;
            }

            if (sendHeaders)
                response.setStatus(jaxrsResponse.getStatus());
            final BuiltResponse built = jaxrsResponse;
            CommitHeaderOutputStream.CommitCallback callback = new CommitHeaderOutputStream.CommitCallback() {
                private boolean committed;

                @Override
                public void commit() {
                    if (committed)
                        return;
                    committed = true;
                    commitHeaders(built, response);
                }
            };
            OutputStream os = sendHeaders ? makeCommitOutputStream(response.getOutputStream(), callback)
                    : response.getOutputStream();

            WriterInterceptor[] writerInterceptors = null;
            if (method != null) {
                writerInterceptors = method.getWriterInterceptors();
            } else if (providerFactory.getServerWriterInterceptorRegistry() != null) {
                writerInterceptors = providerFactory.getServerWriterInterceptorRegistry().postMatch(null, null);
            }

            RESTEasyTracingLogger tracingLogger = RESTEasyTracingLogger.getInstance(request);
            final long timestamp = tracingLogger.timestamp("WI_SUMMARY");

            AbstractWriterInterceptorContext writerContext = new ServerWriterInterceptorContext(writerInterceptors,
                    providerFactory, entity, type, generic, annotations, mt,
                    jaxrsResponse.getMetadata(), os, request, onWriteComplete);

            CompletionStage<Void> writerAction = writerContext.getStarted().whenComplete((v, t) -> {
                tracingLogger.logDuration("WI_SUMMARY", timestamp, writerContext.getProcessedInterceptorCount());

                if (t == null && sendHeaders) {
                    response.setOutputStream(writerContext.getOutputStream()); //propagate interceptor changes on the outputstream to the response
                    callback.commit(); // just in case the output stream is never used
                }
            });

            try {
                writerAction.toCompletableFuture()
                        .getNow(null); // give a chance at non-async exceptions to be propagated up
            } catch (CompletionException x) {
                // make sure we unwrap these horrors
                SynchronousDispatcher.rethrow(x.getCause());
            }
        });
    }

    private static OutputStream makeCommitOutputStream(OutputStream delegate, CommitCallback headers) {
        return delegate instanceof AsyncOutputStream
                ? new CommitHeaderAsyncOutputStream((AsyncOutputStream) delegate, headers)
                : new CommitHeaderOutputStream(delegate, headers);
    }

    public static void setResponseMediaType(BuiltResponse jaxrsResponse, HttpRequest request, HttpResponse response,
            ResteasyProviderFactory providerFactory, ResourceMethodInvoker method) {
        MediaType mt = getResponseMediaType(jaxrsResponse, request, response, providerFactory, method);
        if (mt != null) {
            jaxrsResponse.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, mt);
        }
    }

    public static MediaType getResponseMediaType(BuiltResponse jaxrsResponse, HttpRequest request, HttpResponse response,
            ResteasyProviderFactory providerFactory, ResourceMethodInvoker method) {
        MediaType mt = null;
        if (!jaxrsResponse.isClosed() && jaxrsResponse.getEntity() != null) {
            if ((mt = jaxrsResponse.getMediaType()) == null) {
                mt = getDefaultContentType(request, jaxrsResponse, providerFactory, method);
            }

            boolean addCharset = true;
            ResteasyDeployment deployment = ResteasyContext.getContextData(ResteasyDeployment.class);
            if (deployment != null) {
                addCharset = deployment.isAddCharset();
            }
            if (addCharset) {
                if (!mt.getParameters().containsKey(MediaType.CHARSET_PARAMETER)) {
                    if (MediaTypeHelper.isTextLike(mt)) {
                        mt = mt.withCharset(StandardCharsets.UTF_8.toString());
                    }
                }
            }
        }
        return mt;
    }

    private static void executeFilters(BuiltResponse jaxrsResponse, HttpRequest request, HttpResponse response,
            ResteasyProviderFactory providerFactory,
            ResourceMethodInvoker method, Consumer<Throwable> onComplete, RunnableWithIOException continuation)
            throws IOException {
        ContainerResponseFilter[] responseFilters = null;

        if (method != null) {
            responseFilters = method.getResponseFilters();
        } else {
            responseFilters = providerFactory.getContainerResponseFilterRegistry().postMatch(null, null);
        }

        if (responseFilters != null) {
            ResponseContainerRequestContext requestContext = new ResponseContainerRequestContext(request);
            ContainerResponseContextImpl responseContext = new ContainerResponseContextImpl(request, response, jaxrsResponse,
                    requestContext, responseFilters, onComplete, continuation);

            RESTEasyTracingLogger logger = RESTEasyTracingLogger.getInstance(request);

            final long timestamp = logger.timestamp("RESPONSE_FILTER_SUMMARY");
            // filter calls the continuation
            responseContext.filter();
            logger.logDuration("RESPONSE_FILTER_SUMMARY", timestamp, responseFilters.length);
        } else {
            try {
                continuation.run(onComplete);
            } catch (Throwable t) {
                onComplete.accept(t);
                SynchronousDispatcher.rethrow(t);
            }
        }
    }

    protected static void setDefaultContentType(HttpRequest request, BuiltResponse jaxrsResponse,
            ResteasyProviderFactory providerFactory, ResourceMethodInvoker method) {
        MediaType chosen = getDefaultContentType(request, jaxrsResponse, providerFactory, method);
        jaxrsResponse.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, chosen);
    }

    @SuppressWarnings("rawtypes")
    protected static MediaType getDefaultContentType(final HttpRequest request, final BuiltResponse jaxrsResponse,
            final ResteasyProviderFactory providerFactory, final ResourceMethodInvoker method) {
        // Note. If we get here before the request is executed, e.g., if a ContainerRequestFilter aborts,
        // chosen and method can be null.

        MediaType chosen = (MediaType) request.getAttribute(SegmentNode.RESTEASY_CHOSEN_ACCEPT);
        // TODO (jrp) we need to support this, but for testing purposes I'm leaving it off
        // boolean hasProduces = chosen != null
        //        && Boolean.valueOf(chosen.getParameters().get(SegmentNode.RESTEASY_SERVER_HAS_PRODUCES));
        // Get the response type and generic type
        Class type = jaxrsResponse.getEntityClass();
        Type generic = jaxrsResponse.getGenericType();
        if (generic == null) {
            if (method != null && !Response.class.isAssignableFrom(method.getMethod().getReturnType()))
                generic = method.getGenericReturnType();
            else
                generic = type;
        }
        Annotation[] annotations = jaxrsResponse.getAnnotations();
        if (annotations == null && method != null) {
            annotations = method.getMethodAnnotations();
        }
        // Gather the producible media types, per Jakarta REST 3.1 section 3.8 item 2
        // https://jakarta.ee/specifications/restful-ws/3.1/jakarta-restful-ws-spec-3.1#determine_response_type
        final List<SortableMediaType> P = new ArrayList<>();
        if (method != null && method.hasProduces()) {
            P.addAll(Stream.of(method.getProduces())
                    .map((m) -> new SortableMediaType(m.getType(), m.getSubtype(), m.getParameters(), null)).toList());
        } else if (method != null && method.getMethod().getDeclaringClass().isAnnotationPresent(Produces.class)) {
            P.addAll(Stream.of(method.getMethod().getDeclaringClass().getAnnotation(Produces.class).value())
                    .map(MediaType::valueOf)
                    .map((m) -> new SortableMediaType(m.getType(), m.getSubtype(), m.getParameters(), null)).toList());
        } else {
            // The method nor the class have a @Produces, gather the media types the MessageBodyWriter's produce
            // based on the type, generic type annotations and */*.
            final var mediaTypes = providerFactory.getSupportedMediaTypes(type, generic, annotations, MediaType.WILDCARD_TYPE);
            for (var mediaType : mediaTypes) {
                P.add(new SortableMediaType(mediaType.getType(), mediaType.getSubtype(), mediaType.getParameters(), null));
            }
        }

        // P is empty, P={'*/*'}, step 3
        if (P.isEmpty()) {
            P.add(new SortableMediaType("*", "*", Map.of(), null));
        }

        // Step 4, obtain the acceptable media type defaulting to */* if not found
        final List<MediaType> accepts = request.getHttpHeaders().getAcceptableMediaTypes();
        if (accepts.isEmpty()) {
            accepts.add(MediaType.WILDCARD_TYPE);
        }
        final List<SortableMediaType> M = new ArrayList<>();
        // Step 5, a must be compatible with p, if so add the most compatible media type to M.
        for (MediaType accept : accepts) {
            for (MediaType produce : P) {
                if (accept.isCompatible(produce)) {
                    SortableMediaType ms = mostSpecific(produce, null, accept, null);
                    M.add(ms);
                }
            }
        }
        if (chosen == null) {
            chosen = MediaType.WILDCARD_TYPE;
        }
        // Step 6, 7 and 8. Checks if M is empty and throws a 406 if it is. Sorts the media types, and selects the
        // best option
        chosen = chooseFromM(chosen, M, chosen.isWildcardType(),
                chosen.getSubtype().equalsIgnoreCase("application") && chosen.isWildcardSubtype());
        if (chosen.getParameters().containsKey(SegmentNode.RESTEASY_SERVER_HAS_PRODUCES)) {
            Map<String, String> map = new HashMap<>(chosen.getParameters());
            map.remove(SegmentNode.RESTEASY_SERVER_HAS_PRODUCES);
            map.remove(SegmentNode.RESTEASY_SERVER_HAS_PRODUCES_LC);
            chosen = new MediaType(chosen.getType(), chosen.getSubtype(), map);
        }
        boolean hasQ = chosen.getParameters().containsKey("q");
        boolean hasQs = chosen.getParameters().containsKey("qs");
        if (hasQ || hasQs) {
            Map<String, String> map = new HashMap<>(chosen.getParameters());
            if (hasQ) {
                map.remove("q");
            }
            if (hasQs) {
                map.remove("qs");
            }
            chosen = new MediaType(chosen.getType(), chosen.getSubtype(), map);
        }
        return chosen;
    }

    private static MediaType chooseFromM(MediaType currentChoice, List<SortableMediaType> M, boolean hasStarStar,
            boolean hasApplicationStar) {
        //JAX-RS 2.0 Section 3.8.6
        if (M.isEmpty()) {
            throw new NotAcceptableException();
        }
        //JAX-RS 2.0 Section 3.8.7
        Collections.sort(M);
        //JAX-RS 2.0 Section 3.8.8
        for (SortableMediaType m : M) {
            if (isConcrete(m)) {
                currentChoice = m;
                break;
            }
        }
        if (!isConcrete(currentChoice)) {
            //JAX-RS 2.0 Section 3.8.9
            if (hasStarStar || hasApplicationStar) {
                currentChoice = MediaType.APPLICATION_OCTET_STREAM_TYPE;
            } else {
                //JAX-RS 2.0 Section 3.8.10
                throw new NotAcceptableException();
            }
        }
        return currentChoice;
    }

    private static boolean isConcrete(MediaType m) {
        return m != null && !m.isWildcardType() && !m.isWildcardSubtype();
    }

    public static MediaType resolveContentType(BuiltResponse response) {
        MediaType responseContentType = null;
        Object type = response.getMetadata().getFirst(HttpHeaderNames.CONTENT_TYPE);
        if (type == null) {
            return MediaType.WILDCARD_TYPE;
        }
        if (type instanceof MediaType) {
            responseContentType = (MediaType) type;
        } else {
            responseContentType = MediaType.valueOf(type.toString());
        }
        return responseContentType;
    }

    public static void commitHeaders(BuiltResponse jaxrsResponse, HttpResponse response) {
        if (jaxrsResponse.getMetadata() != null) {
            List<Object> cookies = jaxrsResponse.getMetadata().get(
                    HttpHeaderNames.SET_COOKIE);
            if (cookies != null) {
                Iterator<Object> it = cookies.iterator();
                while (it.hasNext()) {
                    Object next = it.next();
                    if (next instanceof NewCookie) {
                        NewCookie cookie = (NewCookie) next;
                        response.addNewCookie(cookie);
                        it.remove();
                    } else {
                        response.getOutputHeaders().add(jakarta.ws.rs.core.HttpHeaders.SET_COOKIE, next);
                        it.remove();
                    }
                }

                jaxrsResponse.getMetadata().remove(HttpHeaderNames.SET_COOKIE);
            }
        }
        if (jaxrsResponse.getMetadata() != null
                && jaxrsResponse.getMetadata().size() > 0) {
            response.getOutputHeaders().putAll(jaxrsResponse.getMetadata());
        }
    }

    private static class SortableMediaType extends MediaType implements Comparable<SortableMediaType> {
        double q = 1;
        double qs = 1;
        Class<?> writerType = null;

        SortableMediaType(final String type, final String subtype, final Map<String, String> parameters,
                final Class<?> writerType) {
            super(type, subtype, parameters);
            this.writerType = writerType;
            String qString = parameters.get("q");
            if (qString != null) {
                try {
                    q = Double.valueOf(qString);
                } catch (NumberFormatException e) {
                    // skip
                }
            }
            String qsString = parameters.get("qs");
            if (qsString != null) {
                try {
                    qs = Double.valueOf(qsString);
                } catch (NumberFormatException e) {
                    // skip
                }
            }
        }

        @Override
        public int compareTo(SortableMediaType o) {
            if (o.isCompatible(this)) {
                if (o.equals(this)) {
                    return 0;
                }
                return o.equals(selectMostSpecific(o, this)) ? 1 : -1;
            }
            if (o.q < this.q) {
                return -1;
            }
            if (o.q > this.q) {
                return 1;
            }
            // zzzz.q == this.q
            if (o.qs < this.qs) {
                return -1;
            }
            if (o.qs > this.qs) {
                return 1;
            }
            if (o.writerType == this.writerType)
                return 0;
            if (o.writerType == null)
                return -1;
            if (this.writerType == null)
                return 1;
            if (o.writerType.isAssignableFrom(this.writerType))
                return -1;
            if (this.writerType.isAssignableFrom(o.writerType))
                return 1;
            return 0;
        }
    }

    /**
     * m1, m2 are compatible
     */
    private static SortableMediaType selectMostSpecific(SortableMediaType m1, SortableMediaType m2) {
        if (m1.getType().equals("*")) {
            if (m2.getType().equals("*")) {
                if (m1.getSubtype().equals("*")) {
                    return m2; // */* <= */?
                } else {
                    return m1; // */st > */?
                }
            } else {
                return m2; // */? < t/?
            }
        } else {
            if (m2.getType().equals("*")) {
                return m1; // t/? > */?
            } else {
                if (m1.getSubtype().equals("*")) {
                    return m2; // t/* <= t/?
                } else {
                    return m1; // t/st >= t/?
                }
            }
        }
    }

    private static SortableMediaType mostSpecific(MediaType p, Class<?> wtp, MediaType a, Class<?> wta) {
        if (p.getType().equals("*")) {
            if (a.getType().equals("*")) {
                if (p.getSubtype().equals("*")) {
                    return mixAddingQS(a, wta, p); // */* <= */?
                } else {
                    return mixAddingQ(p, wtp, a); // */st > */?
                }
            } else {
                return mixAddingQS(a, wta, p); // */? < t/?
            }
        } else {
            if (a.getType().equals("*")) {
                return mixAddingQ(p, wtp, a); // t/? > */?
            } else {
                if (p.getSubtype().equals("*")) {
                    return mixAddingQS(a, wta, p); // t/* <= t/?
                } else {
                    return mixAddingQ(p, wtp, a); // t/st >= t/?
                }
            }
        }
    }

    private static SortableMediaType mixAddingQ(MediaType p, Class<?> wtp, MediaType a) {
        Map<String, String> pars = p.getParameters();
        String q = a.getParameters().get("q");
        if (q != null) {
            pars = new HashMap<>(pars);
            pars.put("q", q);
        }
        return new SortableMediaType(p.getType(), p.getSubtype(), pars, wtp);
    }

    private static SortableMediaType mixAddingQS(MediaType a, Class<?> wta, MediaType p) {
        Map<String, String> pars = a.getParameters();
        String qs = p.getParameters().get("qs");
        if (qs != null) {
            pars = new HashMap<>(pars);
            pars.put("qs", qs);
        }
        return new SortableMediaType(a.getType(), a.getSubtype(), pars, wta);

    }
}
