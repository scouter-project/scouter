package scouter.plugin.server.kafka.profile;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/** A bounded, best-effort queue. Producers never execute publishing work. */
final class AsyncPublisher {
    private final ArrayBlockingQueue<Runnable> queue;
    private final Thread worker;
    private final Runnable maintenance;
    private volatile boolean closed;
    private volatile Runnable cleanup;

    AsyncPublisher(int capacity) {
        this(capacity, () -> {});
    }

    AsyncPublisher(int capacity, Runnable maintenance) {
        this.maintenance = maintenance;
        queue = new ArrayBlockingQueue<Runnable>(capacity);
        worker = new Thread(this::run, "scouter-kafka-profile-publisher");
        worker.setDaemon(true);
        worker.start();
    }

    synchronized boolean offer(Runnable task) {
        return !closed && queue.offer(task);
    }

    private void run() {
        try {
            while (!closed) {
                maintenance.run();
                Runnable task = queue.poll(1, TimeUnit.SECONDS);
                if (task != null && !closed) {
                    task.run();
                }
            }
        } catch (InterruptedException ignored) {
            // Closing interrupts any current Kafka metadata/buffer wait.
        } finally {
            queue.clear();
            Runnable action = cleanup;
            if (action != null) {
                // Kafka close must not inherit the worker's interrupted flag.
                Thread.interrupted();
                action.run();
            }
        }
    }

    void close(Runnable action) {
        synchronized (this) {
            if (closed) {
                return;
            }
            cleanup = action;
            closed = true;
            queue.clear();
            worker.interrupt();
        }
        try {
            worker.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
