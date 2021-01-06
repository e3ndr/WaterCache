package xyz.e3ndr.watercache.scheduling;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.NonNull;

public class WaterScheduler {
    private Map<Integer, Task> taskQueue = new ConcurrentHashMap<>();

    private ThreadProvider provider;

    private int schedulerIdCounter = 0;
    private boolean running;

    public WaterScheduler() {}

    public WaterScheduler(@NonNull ThreadFactory threadFactory) {
        this.provider = (runnable) -> {
            threadFactory.newThread(runnable).start();
        };
    }

    public WaterScheduler(@NonNull ExecutorService executorService) {
        this.provider = (runnable) -> {
            executorService.submit(runnable);
        };
    }

    public int runAt(@NonNull Runnable runnable, long timeToRunAt) {
        int id = getNewId();

        this.taskQueue.put(id, new Task(runnable, timeToRunAt, id));

        synchronized (this.taskQueue) {
            this.taskQueue.notifyAll();
        }

        return id;
    }

    public synchronized int runAfter(@NonNull Runnable runnable, long amount, @NonNull TimeUnit unit) {
        return this.runAt(runnable, System.currentTimeMillis() + unit.toMillis(amount));
    }

    public void start() {
        if (!this.running) {
            this.running = true;

            (new Thread(() -> {
                while (this.running) {
                    long nextTick = tick();

                    try {
                        long waitFor = nextTick - System.currentTimeMillis();

                        if (waitFor > 0) {
                            synchronized (this.taskQueue) {
                                this.taskQueue.wait(waitFor);
                            }
                        }
                    } catch (InterruptedException ignored) {}
                }
            })).start();
        }
    }

    private long tick() {
        long current = System.currentTimeMillis();
        long nextTick = Long.MAX_VALUE;

        for (Task task : this.taskQueue.values()) {
            if (current >= task.timeToRunAt) {
                if (this.isAsynchronousDispatch()) {
                    this.provider.run(task);
                } else {
                    try {
                        task.run();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            } else if (nextTick != -1) {
                if (task.timeToRunAt < nextTick) {
                    nextTick = task.timeToRunAt;
                }
            }
        }

        return nextTick;
    }

    public void stop() {
        this.running = false;
    }

    public boolean isAsynchronousDispatch() {
        return this.provider != null;
    }

    private synchronized int getNewId() {
        return this.schedulerIdCounter++;
    }

    /* ---------------- */
    /* Internal         */
    /* ---------------- */

    private static interface ThreadProvider {

        public void run(Runnable runnable);

    }

    @AllArgsConstructor
    private class Task implements Runnable {
        private Runnable runnable;
        private long timeToRunAt;
        private int id;

        @Override
        public void run() {
            taskQueue.remove(this.id);
            this.runnable.run();
        }

    }

}
