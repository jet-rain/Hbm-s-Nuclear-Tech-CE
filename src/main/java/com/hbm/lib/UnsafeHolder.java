package com.hbm.lib;

import com.hbm.core.HbmCorePlugin;
import com.hbm.main.MainRegistry;
import org.jetbrains.annotations.ApiStatus;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

@ApiStatus.Internal
public class UnsafeHolder {
    public static final Unsafe U;

    static {
        MainRegistry.logger.info("Loading UnsafeHolder");
        U = getUnsafe();
    }

    private static Unsafe getUnsafe() {
        Unsafe instance;
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            instance = (Unsafe) field.get(null);
        } catch (Exception e) {
            MainRegistry.logger.error("Failed to get theUnsafe in Unsafe.class, trying UnsafeHolder", e);
            try { // Fallback for cleanroom loader but should never happen
                Class<?> holderClass = Class.forName("top.outlands.foundation.boot.UnsafeHolder");
                Field unsafeField = holderClass.getField("UNSAFE");
                instance = (Unsafe) unsafeField.get(null);
            } catch (Exception ignored) {
                try {
                    Constructor<Unsafe> c = Unsafe.class.getDeclaredConstructor();
                    c.setAccessible(true);
                    instance = c.newInstance();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return instance;
    }

    public static long fieldOffset(Class<?> clz, String fieldName) throws RuntimeException {
        try {
            return U.objectFieldOffset(clz.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T allocateInstance(Class<? extends T> clz) {
        try {
            //noinspection unchecked
            return (T) U.allocateInstance(clz);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public static long fieldOffset(Class<?> clz, String mcp, String srg) throws RuntimeException {
        try {
            return U.objectFieldOffset(clz.getDeclaredField(HbmCorePlugin.runtimeDeobfEnabled() ? srg : mcp));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
