package org.jboss.resteasy.client.jaxrs.engines;

import java.util.concurrent.CompletionStage;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.reactivestreams.Publisher;

@Deprecated(forRemoval = true) // TODO (jrp) this needs to be moved to a new module and possibly be renamed
public interface ReactiveClientHttpEngine extends AsyncClientHttpEngine {
    /**
     * This is the main bridge from RestEasy to a reactive implementation.
     */
    <T> Publisher<T> submitRx(ClientInvocation request,
            boolean buffered,
            ResultExtractor<T> extractor);

    <T> Publisher<T> fromCompletionStage(CompletionStage<T> cs);

    <T> Publisher<T> just(T t);

    /**
     * How the reactive implementation handles errors.
     */
    <T> Publisher<T> error(Exception e);
}
