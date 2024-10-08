/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
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

package dev.resteasy.embedded.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.jandex.Index;
import org.jboss.resteasy.core.se.ConfigurationOption;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.api.ConfigurationProvider;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RestBootstrap(value = ServletTest.RootApplication.class, configFactory = ServletTest.InjectionConfiguration.class)
public class ServletTest {
    public static class InjectionConfiguration implements ConfigurationProvider {
        @Override
        public SeBootstrap.Configuration getConfiguration() {
            try {
                final Index index = Index.of(TestServlet.class, RootApplication.class, TestResource.class);
                final DeploymentInfo deploymentInfo = new DeploymentInfo()
                        .addServlet(Servlets.servlet(TestServlet.class)
                                .addMapping("/test-servlet"));
                return SeBootstrap.Configuration.builder()
                        .property(ConfigurationOption.JANDEX_INDEX.key(), index)
                        .property(UndertowConfigurationOptions.DEPLOYMENT_INFO, deploymentInfo)
                        .build();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Test
    public void servlet(final URI uri) throws Exception {
        final HttpClient client = HttpClient.newHttpClient();
        final HttpResponse<String> response = client.send(HttpRequest
                .newBuilder(UriBuilder.fromUri(uri).path("test-servlet").build()).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals("test-servlet", response.body());
    }

    @Test
    public void resource(@RequestPath("/test") final WebTarget target) {
        try (Response response = target.request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("test-resource", response.readEntity(String.class));
        }
    }

    @WebServlet("/test-servlet")
    public static class TestServlet extends HttpServlet {
        @Override
        protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
                throws IOException {
            resp.setContentType(MediaType.TEXT_PLAIN);
            resp.getWriter().print("test-servlet");
            resp.getWriter().flush();
        }
    }

    @ApplicationPath("/")
    public static class RootApplication extends Application {

    }

    @Path("/test")
    public static class TestResource {

        @GET
        @Path("/")
        public Response get() {
            return Response.ok("test-resource").build();
        }
    }
}
