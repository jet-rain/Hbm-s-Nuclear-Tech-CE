package com.hbm.core;

import com.hbm.events.ChunkEventRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

@SuppressWarnings("unused")
public final class ChunkHook {
    private ChunkHook() {}

    public static IBlockState onGetBlockState(Chunk chunk, int x, int y, int z) {
        return ChunkEventRegistry.fireGet(chunk, x, y, z);
    }

    public static IBlockState onSetBlockState(Chunk chunk, BlockPos pos, IBlockState newState) {
        return ChunkEventRegistry.fireSet(chunk, pos, newState);
    }
}
