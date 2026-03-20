package org.jboss.resteasy.test.client;

import java.io.File;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;
import org.jboss.resteasy.test.common.RequestTarget;
import org.jboss.resteasy.test.common.RequiresHttpServer;
import org.jboss.resteasy.utils.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 * @version $Revision: 1 $
 * @tpSubChapter Resteasy-client
 * @tpChapter Unit tests
 * @tpTestCaseDetails Verify request is sent in chunked format
 * @tpSince RESTEasy 3.1.4
 */
@RequiresHttpServer
public class ChunkedTransferEncodingUnitTest {
    private static final String testFilePath;

    static {
        testFilePath = TestUtil.getResourcePath(ChunkedTransferEncodingUnitTest.class, "ChunkedTransferEncodingUnitTestFile");
    }

    @Test
    public void testChunkedTarget(@RequestTarget("chunked") final ResteasyWebTarget target) throws Exception {
        target.setChunked(true);
        ClientInvocationBuilder request = (ClientInvocationBuilder) target.request();
        File file = new File(testFilePath);
        Response response = request.post(Entity.entity(file, "text/plain"));
        String header = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("ok", header);
        response.close();
    }

    @Test
    public void testChunkedRequest(@RequestTarget("chunked") final ResteasyWebTarget target) throws Exception {
        ClientInvocationBuilder request = (ClientInvocationBuilder) target.request();
        request.setChunked(true);
        File file = new File(testFilePath);
        Response response = request.post(Entity.entity(file, "text/plain"));
        String header = response.readEntity(String.class);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("ok", header);
        response.close();
    }
}
