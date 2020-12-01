package org.jboss.resteasy.microprofile.client.impl;

import java.net.URI;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;
import org.jboss.resteasy.client.jaxrs.internal.ClientRequestHeaders;
import org.jboss.resteasy.microprofile.client.async.AsyncInterceptorRxInvoker;

import javax.ws.rs.client.CompletionStageRxInvoker;


public class MpClientInvocationBuilder extends ClientInvocationBuilder {

    public MpClientInvocationBuilder(final ResteasyClient client, final URI uri, final ClientConfiguration configuration) {
        super(client, uri, configuration);
    }

    @Override
    public CompletionStageRxInvoker rx() {
        return new AsyncInterceptorRxInvoker(this, invocation.getClient().asyncInvocationExecutor());
    }

    @Override
    protected ClientInvocation createClientInvocation(ResteasyClient client, URI uri, ClientRequestHeaders headers,
                                                      ClientConfiguration parent) {
        return new MpClientInvocation(client, uri, headers, parent);
    }
}