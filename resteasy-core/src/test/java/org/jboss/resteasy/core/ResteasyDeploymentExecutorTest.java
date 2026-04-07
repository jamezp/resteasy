/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.resteasy.spi.ResteasyDeployment;
import org.junit.jupiter.api.Test;

/**
 * Tests for executor lifecycle management in {@link ResteasyDeploymentImpl}.
 * <p>
 * Verifies that executors are properly created, shared within a deployment, and shut down when the deployment stops to
 * prevent classloader leaks.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @see <a href="https://issues.redhat.com/browse/RESTEASY-3596">RESTEASY-3596</a>
 */
class ResteasyDeploymentExecutorTest {

    /**
     * Tests that the scheduled executor service is properly shut down when the deployment stops. This is critical to
     * prevent classloader leaks.
     */
    @Test
    void scheduledExecutorShutdownOnDeploymentStop() {
        final ResteasyDeployment deployment = createAndStartDeployment();

        // Get the executor (this creates it lazily)
        final ScheduledExecutorService executor = deployment.getScheduledExecutorService();
        assertNotNull(executor, "Scheduled executor should be created");
        assertFalse(executor.isShutdown(), "Executor should not be shutdown initially");

        // Stop the deployment
        deployment.stop();

        // Verify executor was shut down
        assertTrue(executor.isShutdown(), "Executor should be shutdown after deployment stop");

        // Verify we can't get executor after stop
        final IllegalStateException exception = assertThrows(IllegalStateException.class,
                deployment::getScheduledExecutorService,
                "Should throw IllegalStateException when accessing executor after deployment stop");

        assertTrue(exception.getMessage().contains("stopped"),
                "Exception message should indicate deployment is stopped");
    }

    /**
     * Tests that the executor service is properly shut down when the deployment stops.
     */
    @Test
    void executorShutdownOnDeploymentStop() {
        final ResteasyDeployment deployment = createAndStartDeployment();

        // Get the executor (this creates it lazily)
        final ExecutorService executor = deployment.getExecutorService();
        assertNotNull(executor, "Executor should be created");
        assertFalse(executor.isShutdown(), "Executor should not be shutdown initially");

        // Stop the deployment
        deployment.stop();

        // Verify executor was shut down
        assertTrue(executor.isShutdown(), "Executor should be shutdown after deployment stop");

        // Verify we can't get executor after stop
        final IllegalStateException exception = assertThrows(IllegalStateException.class, deployment::getExecutorService,
                "Should throw IllegalStateException when accessing executor after deployment stop");

        assertTrue(exception.getMessage().contains("stopped"),
                "Exception message should indicate deployment is stopped");
    }

    /**
     * Tests that the executor can actually execute tasks before shutdown.
     */
    @Test
    void executorFunctional() throws Exception {
        final ResteasyDeployment deployment = createAndStartDeployment();

        try {
            final ScheduledExecutorService executor = deployment.getScheduledExecutorService();

            // Submit a simple task
            final AtomicBoolean taskRan = new AtomicBoolean(false);
            executor.submit(() -> taskRan.set(true)).get(5, TimeUnit.SECONDS);

            assertTrue(taskRan.get(), "Task should have executed");
        } finally {
            deployment.stop();
        }
    }

    /**
     * Tests that long-running scheduled tasks are interrupted when deployment stops.
     * The graceful shutdown waits up to 10 seconds, then forces shutdown.
     */
    @Test
    void scheduledTasksForcedShutdown() throws Exception {
        final ResteasyDeployment deployment = createAndStartDeployment();

        final ScheduledExecutorService executor = deployment.getScheduledExecutorService();

        // Schedule a task way in the future (beyond the 10 second graceful timeout)
        final AtomicBoolean taskRan = new AtomicBoolean(false);
        executor.schedule(() -> taskRan.set(true), 60, TimeUnit.SECONDS);

        // Stop immediately
        final long startTime = System.currentTimeMillis();
        deployment.stop();
        final long stopTime = System.currentTimeMillis();

        // Shutdown should complete in ~10 seconds (graceful timeout), not 60
        final long shutdownDuration = stopTime - startTime;
        assertTrue(shutdownDuration < 15000,
                () -> String.format("Shutdown should not wait for distant future tasks, took: %sms", shutdownDuration));

        // Wait a moment
        TimeUnit.MILLISECONDS.sleep(500L);

        // Task should not have run (it was scheduled for 60 seconds)
        assertFalse(taskRan.get(), "Long-future task should not run after forced shutdown");
    }

    /**
     * Tests graceful shutdown behavior - tasks in progress should complete.
     */
    @Test
    void gracefulShutdown() throws Exception {
        final ResteasyDeployment deployment = createAndStartDeployment();

        final ScheduledExecutorService executor = deployment.getScheduledExecutorService();

        final CountDownLatch latch = new CountDownLatch(1);

        // Submit a task that takes a moment
        final AtomicBoolean taskRan = new AtomicBoolean(false);
        executor.submit(() -> {
            try {
                latch.countDown();
                TimeUnit.MILLISECONDS.sleep(100L); // Simulate work
                taskRan.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Wait for the task to start
        assertTrue(latch.await(2L, TimeUnit.SECONDS), "Tasked failed to start");
        // Stop deployment (should wait for task to complete)
        deployment.stop();

        // Task should have completed gracefully
        assertTrue(taskRan.get(), "Task should complete during graceful shutdown");
    }

    private static ResteasyDeployment createAndStartDeployment() {
        final ResteasyDeployment deployment = new ResteasyDeploymentImpl();
        deployment.start();
        return deployment;
    }
}
