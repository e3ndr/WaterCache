package xyz.e3ndr.watercache.watchdog;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import xyz.e3ndr.watercache.Watchable;

@Getter
@RequiredArgsConstructor
@Accessors(chain = true)
public class Watchdog {
    private static final double nanoToMillis = 1e-6;

    // Constructor
    private @NonNull Watchable watchable;
    private final long tickInterval;
    private final long maxTickTime;

    private volatile boolean ticking = false;
    private boolean running = false;
    private Thread tickThread;
    private WatchdogListener listener = new WatchdogListener() {
    };

    public Watchdog setListener(@NonNull WatchdogListener listener) {
        this.listener = listener;

        return this;
    }

    public void start() {
        if (!this.running) {
            Thread thread = (new Thread(() -> {
                this.startBlocking();
            }));

            thread.setName("Watchdog Async Thread");
            thread.start();
        } else {
            throw new IllegalStateException("Watchdog is running.");
        }
    }

    public void startBlocking() {
        if (!this.running) {
            this.watchable.setup();

            this.running = true;

            this.tickThread = new Thread() {
                @SneakyThrows
                @Override
                public void run() {
                    // While running, it will tick the cache, then set ticking to false, then notify all monitors and then wait itself.
                    while (running) {
                        watchable.tick();
                        ticking = false;

                        synchronized (tickThread) {
                            this.notifyAll();
                            this.wait();
                        }
                    }
                }
            };

            this.tickThread.start();

            long start;
            while (this.running) {
                try {
                    start = System.nanoTime();
                    this.ticking = true;

                    // Wake the thread, it'll tick the cache.
                    this.wake();
                    try {
                        // Wait on the thread object until either notify or the maxTickTime has been reached.
                        synchronized (this.tickThread) {
                            this.tickThread.wait(this.maxTickTime);
                        }
                    } catch (InterruptedException e) {
                        this.listener.exception(new Exception("Unable to wait for tickthread", e));
                    }

                    // Alert the listener that the cache is taking too long to tick.
                    if (this.ticking) {
                        this.listener.onNotResponding((System.nanoTime() - start) * nanoToMillis);
                        try {
                            // Wait on the cache if it is still ticking at this point, we do this on the tick thread in order to guarantee no deadlocks.
                            synchronized (this.tickThread) {
                                if (this.ticking) {
                                    this.tickThread.wait();
                                }
                            }
                        } catch (InterruptedException e) {
                            this.listener.exception(new Exception("Unable to wait on tickthread", e));
                        }
                        this.listener.onResponding((System.nanoTime() - start) * nanoToMillis);
                    }

                    double timeTicking = (System.nanoTime() - start) * nanoToMillis;
                    long sleepFor = (long) (this.tickInterval - timeTicking);

                    if (sleepFor < 0) {
                        long ticksSkipped = (long) (timeTicking / this.tickInterval);
                        this.listener.onTickSkip(ticksSkipped, timeTicking);
                    } else {
                        try {
                            Thread.sleep(sleepFor);
                        } catch (InterruptedException e) {
                            this.stop();
                            this.listener.exception(new Exception("Unable to sleep", e));
                        }
                    }
                } catch (Exception e) { // Catch all listener exceptions, and then print.
                    new Exception("A listener produced an excpetion", e).printStackTrace();
                }
            }

            // Tell the thread to wake up, since running is false it will exit.
            this.wake();
        } else {
            throw new IllegalStateException("Watchdog is running.");
        }
    }

    private void wake() {
        if ((this.tickThread != null) && this.tickThread.isAlive()) {
            synchronized (this.tickThread) {
                this.tickThread.notifyAll();
            }
        }
    }

    public void stop() {
        this.running = false;
    }

    public boolean isExecutingOnTickThread() {
        if ((this.tickThread == null) || !this.tickThread.isAlive()) {
            throw new IllegalStateException("Watchdog is not currently running.");
        } else {
            return Thread.currentThread() == this.tickThread;
        }
    }

}
