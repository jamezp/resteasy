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

package org.jboss.resteasy.spi.deployment;

import static java.lang.System.getSecurityManager;
import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;

import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ContextClassLoaderDeploymentContextSelector implements DeploymentContextSelector {

    private static final Permission REGISTER_DEPLOYMENT_CONTEXT_PERMISSION = new RuntimePermission("registerDeploymentContext", null);
    private static final Permission UNREGISTER_DEPLOYMENT_CONTEXT_PERMISSION = new RuntimePermission("unregisterDeploymentContext", null);

    private final ConcurrentMap<ClassLoader, DeploymentContext> contextMap = new ConcurrentHashMap<>();

    private final PrivilegedAction<DeploymentContext> deploymentContextAction;

    public ContextClassLoaderDeploymentContextSelector(final DeploymentContextSelector defaultSelector) {
        deploymentContextAction = () -> {
            ClassLoader cl = currentThread().getContextClassLoader();
            if (cl != null) {
                final DeploymentContext mappedContext = contextMap.get(cl);
                if (mappedContext != null) {
                    return mappedContext;
                }
            }
            return defaultSelector.get();
        };
    }

    @Override
    public DeploymentContext get() {
        return System.getSecurityManager() == null ? deploymentContextAction.run() :
                doPrivileged(deploymentContextAction);
    }

    /**
     * Register a class loader with a deployment context. This method requires the {@code registerDeploymentContext} {@link RuntimePermission}.
     *
     * @param classLoader       the classloader
     * @param deploymentContext the deployment context
     *
     * @throws IllegalArgumentException if the classloader is already associated with a deployment context
     */
    public void registerDeploymentContext(ClassLoader classLoader, DeploymentContext deploymentContext)
            throws IllegalArgumentException {
        final SecurityManager sm = getSecurityManager();
        if (sm != null) {
            sm.checkPermission(REGISTER_DEPLOYMENT_CONTEXT_PERMISSION);
        }
        // TODO (jrp) how do we handle cases where multiple deployments, e.g. more than one Application implementation, is processed on the same class loader?
        // TODO (jrp) really, they could just share it. The only possibly issue could be if we start supporting properties defined in the Application.getProperties()
        // TODO (jrp) this would require a greater context in general though
        if (contextMap.putIfAbsent(classLoader, deploymentContext) != null) {
            // TODO (jrp) something needs to be done here, but I'm not sure what yet. We really need a way to have one context per ResteasyDeployment.
            // TODO (jrp) however, we need some kind of way to lookup the context. The TCCL seems to be the best option. Not sure how else to say
            // TODO (jrp) which resources belong to which application. In some cases resources really could belong to any number of applications.
            //throw new IllegalArgumentException("ClassLoader instance is already registered to a deployment context (" + classLoader + ")");
        }
    }

    /**
     * Unregister a class loader/deployment context association.  This method requires the {@code unregisterDeploymentContext} {@link RuntimePermission}.
     *
     * @param classLoader       the classloader
     * @param deploymentContext the deployment context
     *
     * @return {@code true} if the association exists and was removed, {@code false} otherwise
     */
    public boolean unregisterDeploymentContext(ClassLoader classLoader, DeploymentContext deploymentContext) {
        final SecurityManager sm = getSecurityManager();
        if (sm != null) {
            sm.checkPermission(UNREGISTER_DEPLOYMENT_CONTEXT_PERMISSION);
        }
        return contextMap.remove(classLoader, deploymentContext);
    }
}
