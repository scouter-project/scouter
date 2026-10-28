package scouter.plugin.server.kafka.profile;

import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class AsyncPublisherTest {
    @Test(timeout = 5000)
    public void fullQueueRejectsWithoutRunningWorkOnCaller() throws Exception {
        AsyncPublisher publisher = new AsyncPublisher(1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Thread> publishingThread = new AtomicReference<Thread>();
        AtomicInteger queuedRuns = new AtomicInteger();
        try {
            assertTrue(publisher.offer(() -> {
                publishingThread.set(Thread.currentThread());
                started.countDown();
                await(release);
            }));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertNotSame(Thread.currentThread(), publishingThread.get());
            assertTrue(publisher.offer(queuedRuns::incrementAndGet));
            assertFalse(publisher.offer(() -> fail("Rejected work must never execute")));
            assertEquals(0, queuedRuns.get());
        } finally {
            publisher.close(() -> {});
            release.countDown();
        }
        assertEquals(0, queuedRuns.get());
    }

    @Test(timeout = 5000)
    public void closeInterruptsWorkDropsQueueAndCleansUpOnce() throws Exception {
        AsyncPublisher publisher = new AsyncPublisher(2);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        AtomicInteger cleaned = new AtomicInteger();
        AtomicInteger queuedRuns = new AtomicInteger();
        assertTrue(publisher.offer(() -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
            }
        }));
        assertTrue(started.await(2, TimeUnit.SECONDS));
        assertTrue(publisher.offer(queuedRuns::incrementAndGet));
        publisher.close(() -> {
            assertFalse(Thread.currentThread().isInterrupted());
            cleaned.incrementAndGet();
        });
        publisher.close(cleaned::incrementAndGet);
        assertEquals(0, interrupted.getCount());
        assertEquals(0, queuedRuns.get());
        assertEquals(1, cleaned.get());
        assertFalse(publisher.offer(queuedRuns::incrementAndGet));
    }

    @Test(timeout = 5000)
    public void workerPreservesSubmissionOrder() throws Exception {
        AsyncPublisher publisher = new AsyncPublisher(3);
        CountDownLatch done = new CountDownLatch(3);
        StringBuilder order = new StringBuilder();
        try {
            for (int i = 0; i < 3; i++) {
                final int value = i;
                assertTrue(publisher.offer(() -> {
                    order.append(value);
                    done.countDown();
                }));
            }
            assertTrue(done.await(2, TimeUnit.SECONDS));
            assertEquals("012", order.toString());
        } finally {
            publisher.close(() -> {});
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
