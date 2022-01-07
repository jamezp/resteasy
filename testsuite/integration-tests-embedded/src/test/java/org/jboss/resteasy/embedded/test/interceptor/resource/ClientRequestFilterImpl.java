package org.jboss.resteasy.embedded.test.interceptor.resource;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class ClientRequestFilterImpl implements ClientRequestFilter
{

   @Override
   public void filter(ClientRequestContext requestContext) throws IOException
   {
      requestContext.abortWith(Response.status(456).build());
   }
}