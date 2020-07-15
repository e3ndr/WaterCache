package xyz.e3ndr.watercache;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import xyz.e3ndr.watercache.cachable.Cachable;
import xyz.e3ndr.watercache.cachable.DisposeReason;

@Accessors(chain = true)
public class WaterCache {
    private long currentId = 1;
    private Map<Long, Cachable> cache = new ConcurrentHashMap<>();
    private boolean running = false;
    private @Setter CacheEventListener listener = new CacheEventListener() {
    };

    private synchronized long getNextId() {
        return this.currentId++;
    }

    @SneakyThrows
    public long register(@NonNull Cachable item) {
        Field field = Cachable.class.getDeclaredField("id");
        long id = getNextId();

        field.setAccessible(true);
        field.set(item, id);
        this.cache.put(id, item);
        item.onRegister(this);

        return id;
    }

    public Cachable getItem(long id) {
        return this.cache.get(id);
    }

    public boolean has(long id) {
        return this.cache.containsKey(id);
    }

    public boolean has(Cachable item) {
        return this.cache.containsKey(item.getId());
    }

    public boolean dispose(long id) {
        Cachable item = this.cache.remove(id);

        if (item != null) {
            item.onDispose(DisposeReason.MANUAL);
            return true;
        } else {
            return false;
        }
    }

    public boolean dispose(@NonNull Cachable item) {
        if (this.cache.remove(item.getId()) != null) {
            item.onDispose(DisposeReason.MANUAL);
            return true;
        } else {
            return false;
        }
    }

    public void start(TimeUnit unit, long amount) {
        this.start(unit.toMillis(amount));
    }

    public void start(long tickInterval) {
        if (!this.running) {
            this.running = true;

            (new Thread() {
                @SneakyThrows
                @Override
                public void run() {
                    while (running) {
                        long start = System.currentTimeMillis();
                        tick();

                        long timeTicking = System.currentTimeMillis() - start;
                        long difference = tickInterval - timeTicking;

                        if (difference > 0) {
                            Thread.sleep(difference);
                        }
                    }
                }
            }).start();
        }
    }

    public void stop() {
        this.running = false;
    }

    public void tick() {
        // TODO better iteration. possibly an option for parallel threads?
        for (Cachable item : this.cache.values()) {
            try {
                if (item.getExpireTime() <= System.currentTimeMillis()) {
                    if (item.onDispose(DisposeReason.EXPIRED)) {
                        this.cache.remove(item.getId());
                    }
                } else {
                    item.tick();
                }
            } catch (Exception e) {
                this.listener.onTickException(new TickException(e));
            }
        }
    }

}
