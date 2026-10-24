package com.sistemaventas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CatalogRefreshCoordinatorTest {

    @Test
    void publishesToSubscribersOnTheEdtAndStopsAfterUnsubscribe() throws Exception {
        CatalogRefreshCoordinator coordinator = new CatalogRefreshCoordinator();
        CountDownLatch published = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        AtomicBoolean onEdt = new AtomicBoolean();
        AutoCloseable subscription = coordinator.subscribe(() -> {
            calls.incrementAndGet();
            onEdt.set(javax.swing.SwingUtilities.isEventDispatchThread());
            published.countDown();
        });

        Thread worker = new Thread(coordinator::publish);
        worker.start();
        worker.join();
        assertTrue(published.await(2, TimeUnit.SECONDS));
        assertTrue(onEdt.get());
        subscription.close();

        coordinator.publish();
        javax.swing.SwingUtilities.invokeAndWait(() -> { });
        assertEquals(1, calls.get());
    }
}
