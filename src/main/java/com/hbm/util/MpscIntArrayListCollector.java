package com.hbm.util;

import com.hbm.lib.UnsafeHolder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.NotNull;

import static com.hbm.lib.UnsafeHolder.U;

/**
 * A lock-free, thread-safe MPSC {@link IntArrayList} stack that supports atomic drain.
 *
 * @author mlbv
 */
public class MpscIntArrayListCollector {
    private static final long HEAD_OFF = UnsafeHolder.fieldOffset(MpscIntArrayListCollector.class, "head");
    private Node head;

    public void push(int i) {
        while (true) {
            Node h = (Node) U.getObjectVolatile(this, HEAD_OFF);
            Node n = new Node(i, h);
            if (U.compareAndSwapObject(this, HEAD_OFF, h, n)) return;
        }
    }

    @NotNull
    public IntArrayList drain() {
        Node h = (Node) U.getAndSetObject(this, HEAD_OFF, null);
        IntArrayList out = new IntArrayList();
        for (Node p = h; p != null; p = p.next) out.add(p.v);
        return out;
    }

    private static final class Node {
        private final int v;
        private final Node next;

        Node(int v, Node next) {
            this.v = v;
            this.next = next;
        }
    }
}
