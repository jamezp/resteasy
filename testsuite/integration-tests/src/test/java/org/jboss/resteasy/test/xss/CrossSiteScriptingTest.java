/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.resteasy.test.xss;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
// TODO (jrp) test a redirect
@RunWith(Arquillian.class)
@RunAsClient
public class CrossSiteScriptingTest {
    @ArquillianResource
    private URL url;

    private Client client;

    @Deployment
    public static Archive<?> deploySimpleResource() {
        final WebArchive war = TestUtil.prepareArchive(CrossSiteScriptingTest.class.getSimpleName())
                .addAsLibraries(Maven.resolver().resolve("org.owasp.encoder:encoder:1.2.3").withoutTransitivity().asFile());
        return TestUtil.finishContainerPrepare(war, null, GreetingResource.class);
    }

    @Before
    public void createClient() {
        client = ClientBuilder.newClient();
    }

    @After
    public void closeClient() {
        if (client != null) client.close();
    }

    @Test
    public void pathParamGet() throws Exception {
        final String url = createUrl("param", "<script>console.log('attacked')</script>");
        final Response response = client.target(url)
                .request()
                .get();
        validateResponse(response);
    }

    @Test
    public void queryParamGet() throws Exception {
        final String url = createUrl(Collections.singletonMap("name", "<script>console.log('attacked')</script>"), "query");
        final Response response = client.target(url)
                .request()
                .get();
        validateResponse(response);
    }

    @Test
    public void pathParamGetHtml() throws Exception {
        // TODO (jrp) is passing HTML like this valid?
        final String url = createUrl("html", "<script>console.log('attacked')</script>");
        final Response response = client.target(url)
                .request()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML)
                .get();
        validateResponse(response);
    }

    @Test
    public void pathParamPost() throws Exception {
        final String url = createUrl("param", "post", "<script>console.log('attacked')</script>");
        final Response response = client.target(url)
                .request()
                .post(null);
        validateResponse(response);
    }

    @Test
    public void postEntityPlainText() throws Exception {
        final String url = createUrl("entity");
        final Response response = client.target(url)
                .request()
                .post(Entity.entity("<script>console.log('attacked')</script>", MediaType.TEXT_PLAIN_TYPE));
        validateResponse(response);
    }


    @Test
    public void postEntityHtml() throws Exception {
        final String url = createUrl("entity");
        final Response response = client.target(url)
                .request()
                .post(Entity.entity("<script>console.log('attacked')</script>", MediaType.TEXT_HTML_TYPE));
        validateResponse(response);
    }
    // TODO (jrp) test with a Link headerFormEntityTest

    private void validateResponse(final Response response) {
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        final String asText = response.readEntity(String.class);

        Assert.assertNotNull(asText);

        final Document document = Jsoup.parse(asText);
        Assert.assertTrue("Expected no script tags: " + asText, document.select("script").isEmpty());
        Assert.assertEquals("<h1>Hello &lt;script&gt;console.log(&#39;attacked&#39;)&lt;/script&gt;</h1>", asText);
    }

    private String createUrl(final String... paths) throws UnsupportedEncodingException {
        final StringBuilder result = new StringBuilder(url.toString());
        if (result.charAt(result.length() - 1) != '/') {
            result.append("/greet");
        } else {
            result.append("greet");
        }
        for (String path : paths) {
            if (result.charAt(result.length() - 1) != '/') {
                result.append('/');
            }
            result.append(encode(path));
        }

        return result.toString();
    }

    private String createUrl(final Map<String, String> queryParams, final String... paths) throws UnsupportedEncodingException {
        final StringBuilder result = new StringBuilder(url.toString());
        if (result.charAt(result.length() - 1) != '/') {
            result.append("/greet");
        } else {
            result.append("greet");
        }
        for (String path : paths) {
            if (result.charAt(result.length() - 1) != '/') {
                result.append('/');
            }
            result.append(path);
        }
        result.append('?');
        final Iterator<Map.Entry<String, String>> iter = queryParams.entrySet().iterator();
        while (iter.hasNext()) {
            final Map.Entry<String, String> entry = iter.next();
            result.append(encode(entry.getKey()))
                    .append('=')
                    .append(encode(entry.getValue()));
            if (iter.hasNext()) {
                result.append('&');
            }
        }
        return result.toString();
    }

    private static String encode(final String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "utf-8");
    }
}
