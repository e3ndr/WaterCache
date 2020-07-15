package xyz.e3ndr.watercache.watchdog;

public interface WatchdogListener {

    default void onNotResponding(double ms) {
        System.err.println(String.format("WaterCache has not responded for %.4fms", ms));
    }

    default void onResponding(double ms) {
        System.err.println(String.format("WaterCache started responding after ticking for %.4fms", ms));
    }

    default void onTickSkip(long ticksSkipped, double ms) {
        System.err.println(String.format("%.4fms behind! Skipping %d ticks!", ms, ticksSkipped));
    }

    default void exception(Exception e) {
        e.printStackTrace();
    }

}
