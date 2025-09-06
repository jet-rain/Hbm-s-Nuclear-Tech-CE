package com.hbm.lib.maps;

@FunctionalInterface
public interface LongObjectRefConsumer<V, R> {
    void accept(long key, V value, R ref);
}
