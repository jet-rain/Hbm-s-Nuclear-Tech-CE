package com.hbm.events.interfaces;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Proxy;

public interface IChunkEventListener {
    /**
     * Sentinel meaning “short-circuit with null”. DO NOT dereference.
     */
    @SuppressWarnings("unused")
    IBlockState RETURN_NULL =
            (IBlockState) Proxy.newProxyInstance(IBlockState.class.getClassLoader(), new Class<?>[]{IBlockState.class}, (proxy, method, args) -> switch (method.getName()) {
                case "toString" -> "IChunkEventListener.RETURN_NULL";
                case "hashCode" -> 1959784951; // "invalid"
                default -> throw new UnsupportedOperationException("RETURN_NULL sentinel was dereferenced: " + method);
            });

    /**
     * Called when a block state is being retrieved from a chunk.
     *
     * @param chunk The chunk being accessed.
     * @param x     The x coordinate of the block.
     * @param y     The y coordinate of the block.
     * @param z     The z coordinate of the block.
     * @return non-null to short-circuit and use your state; null to fall through to vanilla.
     * @implNote Implementations must be reentrancy-safe and thread-safe.
     */
    @Nullable
    default IBlockState onGetBlockState(Chunk chunk, int x, int y, int z) {
        return null;
    }

    /**
     * Called when a block state is being set in a chunk.
     *
     * @param chunk    The chunk being accessed.
     * @param pos      The position of the block being set.
     * @param newState The new block state.
     * @return non-null to short-circuit; returning {@link #RETURN_NULL} specifically means
     * “short-circuit and return null to the original caller (i.e., no change by vanilla contract)”
     * @implNote Implementations must be reentrancy-safe and thread-safe.
     */
    @Nullable
    default IBlockState onSetBlockState(Chunk chunk, BlockPos pos, IBlockState newState) {
        return null;
    }
}
