package xyz.e3ndr.watercache;

public interface CacheEventListener {

    default void onTickException(TickException e) {
        e.printStackTrace();
    }

}
