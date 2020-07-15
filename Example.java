package xyz.e3ndr.watercache;

import lombok.SneakyThrows;
import xyz.e3ndr.watercache.cachable.Cachable;
import xyz.e3ndr.watercache.watchdog.Watchdog;

public class Example {

    public static void main(String[] args) {
        WaterCache cache = new WaterCache();
        Watchdog wd = new Watchdog(cache, 50, 500);

        Cachable item = new Cachable() {
            private long waitTime = 0;

            @SneakyThrows
            @Override
            public void tick() {
                System.out.println("Sleeping for " + this.waitTime);
                Thread.sleep(this.waitTime);
                this.waitTime += 100;
            }
        };

        cache.register(item);
        wd.start();
    }

}
