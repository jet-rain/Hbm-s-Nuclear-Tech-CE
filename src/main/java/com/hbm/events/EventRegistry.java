package com.hbm.events;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class EventRegistry<T> {

    private final Class<T> elementType;
    private volatile T[] listeners;

    @SuppressWarnings("unchecked")
    public EventRegistry(@NotNull Class<T> elementType) {
        this.elementType = Objects.requireNonNull(elementType, "elementType");
        this.listeners = (T[]) Array.newInstance(elementType, 0);
    }

    public static <T> EventRegistry<T> of(Class<T> elementType) {
        return new EventRegistry<>(elementType);
    }

    public void register(@NotNull T listener) {
        Objects.requireNonNull(listener, "listener");
        T[] src = listeners;
        int n = src.length;
        T[] out = newArray(n + 1);
        System.arraycopy(src, 0, out, 0, n);
        out[n] = listener;
        listeners = out;
    }

    public void unregister(@NotNull T listener) {
        Objects.requireNonNull(listener, "listener");
        T[] src = listeners;
        int n = src.length;
        int matches = 0;
        for (int i = 0; i < n; i++) if (src[i] == listener) matches++;
        if (matches == 0) return;

        T[] out = newArray(n - matches);
        int j = 0;
        for (int i = 0; i < n; i++) {
            T e = src[i];
            if (e != listener) out[j++] = e;
        }
        listeners = out;
    }

    public boolean hasListeners() {
        return listeners.length != 0;
    }

    public void forEach(Consumer<? super T> action) {
        T[] arr = listeners;
        for (int i = 0, n = arr.length; i < n; i++) action.accept(arr[i]);
    }

    public <R> R firstNonNull(Function<? super T, R> invoker) {
        T[] arr = listeners;
        for (int i = 0, n = arr.length; i < n; i++) {
            R r = invoker.apply(arr[i]);
            if (r != null) return r;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private T[] newArray(int size) {
        return (T[]) Array.newInstance(elementType, size);
    }

    public static <I> boolean overrides(I impl, Class<I> iface, String name, Class<?>... params) {
        Objects.requireNonNull(impl, "impl");
        Objects.requireNonNull(iface, "iface");
        Objects.requireNonNull(name, "name");
        try {
            Method m = impl.getClass().getMethod(name, params);
            return m.getDeclaringClass() != iface;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
