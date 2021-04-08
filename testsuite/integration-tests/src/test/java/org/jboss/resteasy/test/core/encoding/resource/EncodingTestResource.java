package org.jboss.resteasy.test.core.encoding.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/test")
// TODO (jrp) we might need to fix the test itself
@Consumes(MediaType.TEXT_HTML)
public class EncodingTestResource {
   @GET
   @Produces("text/plain")
   @Path("/path-param/{pathParam}")
   public String getPathParam(@PathParam("pathParam") String pathParam) {
      return pathParam;
   }


   @GET
   @Produces("text/plain")
   @Path("/query-param")
   public String getQueryParam(@QueryParam("queryParam") String queryParam) {
      return queryParam;
   }
}
