package com.hbm.util;

import com.hbm.lib.Library;
import net.minecraft.util.math.ChunkPos;

import javax.annotation.concurrent.Immutable;

/**
 * Unique identifier for sub-chunks.
 *
 * @author mlbv
 */
@Immutable
public class SubChunkKey {
    private static final int CHUNK_BITS = 28;
    private static final int SUBY_BITS = 64 - 2 * CHUNK_BITS;
    private static final long SUBY_MASK = (1L << SUBY_BITS) - 1;
    private static final long CHUNK_MASK = (1L << CHUNK_BITS) - 1;

    private final int chunkXPos;
    private final int chunkZPos;
    private final int subY;
    private final long packed;
    private final int hash;

    public SubChunkKey(int cx, int cz, int sy) {
        this.chunkXPos = cx;
        this.chunkZPos = cz;
        this.subY = sy;
        this.packed = asLong(cx, cz, sy);
        int result = subY;
        result = 31 * result + cx;
        result = 31 * result + cz;
        this.hash = result;
    }

    public SubChunkKey(long longSubKey){
        this(getSubX(longSubKey), getSubZ(longSubKey), getSubY(longSubKey));
    }

    public SubChunkKey(ChunkPos pos, int sy) {
        this(pos.x, pos.z, sy);
    }

    @Override
    public final int hashCode() {
        return this.hash;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubChunkKey k)) return false;
        return this.subY == k.subY && this.chunkXPos == k.chunkXPos && this.chunkZPos == k.chunkZPos;
    }

    public int getSubY() {
        return subY;
    }

    public int getChunkXPos() {
        return chunkXPos;
    }

    public int getChunkZPos() {
        return chunkZPos;
    }

    public ChunkPos getPos() {
        return new ChunkPos(this.chunkXPos, this.chunkZPos);
    }
    
    public long getPosLong() {
        return ChunkPos.asLong(chunkXPos, chunkZPos);
    }

    public long asLong() {
        return packed;
    }

    private static long zz(int v) {
        return ((long) v << 1) ^ (v >> 31);
    }

    private static int unzz(long u) {
        return (int) ((u >>> 1) ^ -(u & 1L));
    }

    public static long fromPackedBlockPosLong(long serialized) {
        final int cx = Library.getBlockPosX(serialized) >> 4;
        final int cz = Library.getBlockPosZ(serialized) >> 4;
        final int sy = Library.getBlockPosY(serialized) >> 4;
        return asLong(cx, cz, sy);
    }

    public static long asLong(int cx, int cz, int subY) {
        long ux = zz(cx);
        long uz = zz(cz);
        if ((ux & ~CHUNK_MASK) != 0L || (uz & ~CHUNK_MASK) != 0L || (long) subY < 0L || (long) subY > SUBY_MASK)
            throw new IllegalArgumentException("Out of range: cx=" + cx + " cz=" + cz + " sy=" + subY);
        return ux | (uz << CHUNK_BITS) | (((long) subY & SUBY_MASK) << (2 * CHUNK_BITS));
    }

    public static int getSubX(long longSubKey) {
        return unzz(longSubKey & CHUNK_MASK);
    }

    public static int getSubZ(long longSubKey) {
        return unzz((longSubKey >>> CHUNK_BITS) & CHUNK_MASK);
    }

    public static int getSubY(long longSubKey) {
        return (int) ((longSubKey >>> (2 * CHUNK_BITS)) & SUBY_MASK);
    }

    public static ChunkPos getPos(long longSubKey) {
        return new ChunkPos(getSubX(longSubKey), getSubZ(longSubKey));
    }

    public static long getPosLong(long longSubKey) {
        return ChunkPos.asLong(SubChunkKey.getSubX(longSubKey), SubChunkKey.getSubZ(longSubKey));
    }
}
