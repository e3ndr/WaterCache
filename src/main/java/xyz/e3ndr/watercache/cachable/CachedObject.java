package xyz.e3ndr.watercache.cachable;

import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class CachedObject<T> extends Cachable {
    private @NonNull T object;

    public CachedObject() {}

    public CachedObject(long life) {
        super(life);
    }

    public CachedObject(TimeUnit unit, long amount) {
        super(unit.toMillis(amount));
    }

    public T get() {
        return this.object;
    }

}
