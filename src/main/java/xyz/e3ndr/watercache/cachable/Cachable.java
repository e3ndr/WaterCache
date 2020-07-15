package xyz.e3ndr.watercache.cachable;

import java.util.concurrent.TimeUnit;

import lombok.Getter;
import xyz.e3ndr.watercache.WaterCache;

@Getter
public abstract class Cachable {
    private final long timeCreated = System.currentTimeMillis();
    private long id = -1;
    protected long life = -1;

    public Cachable() {}

    public Cachable(long life) {
        this.life = life;
    }

    public Cachable(TimeUnit unit, long amount) {
        this(unit.toMillis(amount));
    }

    public final long getExpireTime() {
        if (this.life >= 0) {
            return this.timeCreated + this.life;
        } else {
            return Long.MAX_VALUE;
        }
    }

    /* Override as needed */
    public void tick() {}

    /* Override as needed */
    public void onRegister(WaterCache cache) {}

    /* Override as needed */
    public boolean onDispose(DisposeReason reason) {
        return true;
    }

}
