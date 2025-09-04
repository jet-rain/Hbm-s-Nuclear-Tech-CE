package com.hbm.util;

import com.hbm.lib.UnsafeHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.hbm.lib.UnsafeHolder.U;

/**
 * A lock-free, thread-safe MPSC stack that supports atomic drain.
 *
 * @author mlbv
 */
public class MpscCollector<T> {
    private static final long HEAD_OFF = UnsafeHolder.fieldOffset(MpscCollector.class, "head");
    private Node<T> head;

    public void push(T value) {
        while (true) {
            // noinspection unchecked
            Node<T> h = (Node<T>) U.getObjectVolatile(this, HEAD_OFF);
            Node<T> n = new Node<>(value, h);
            if (U.compareAndSwapObject(this, HEAD_OFF, h, n)) return;
        }
    }

    @NotNull
    public List<T> drain() {
        // noinspection unchecked
        Node<T> h = (Node<T>) U.getAndSetObject(this, HEAD_OFF, null);
        ArrayList<T> out = new ArrayList<>();
        for (Node<T> p = h; p != null; p = p.next) out.add(p.v);
        return out;
    }

    private static final class Node<T> {
        private final T v;
        private final Node<T> next;

        Node(T v, Node<T> next) {
            this.v = v;
            this.next = next;
        }
    }
}
