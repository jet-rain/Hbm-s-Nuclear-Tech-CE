package com.hbm.events;

import com.hbm.events.interfaces.IChunkEventListener;
import com.hbm.interfaces.ServerThread;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

import static com.hbm.events.EventRegistry.overrides;

public final class ChunkEventRegistry {
    private static final EventRegistry<IChunkEventListener> GET = EventRegistry.of(IChunkEventListener.class);
    private static final EventRegistry<IChunkEventListener> SET = EventRegistry.of(IChunkEventListener.class);

    @ApiStatus.Internal
    public static volatile boolean hasActiveListener = false;

    private ChunkEventRegistry() {
    }

    /**
     * Register a listener; only added to registries for methods it actually overrides.
     */
    @ServerThread
    public static void register(IChunkEventListener listener) {
        Objects.requireNonNull(listener, "listener");
        boolean hasGet = overrides(listener, IChunkEventListener.class, "onGetBlockState", Chunk.class, int.class, int.class, int.class);
        boolean hasSet = overrides(listener, IChunkEventListener.class, "onSetBlockState", Chunk.class, BlockPos.class, IBlockState.class);

        if (hasGet) GET.register(listener);
        if (hasSet) SET.register(listener);
        hasActiveListener = GET.hasListeners() || SET.hasListeners();
    }

    /**
     * Unregister a listener from all event registries.
     */
    @ServerThread
    public static void unregister(IChunkEventListener listener) {
        Objects.requireNonNull(listener, "listener");
        GET.unregister(listener);
        SET.unregister(listener);
        hasActiveListener = GET.hasListeners() || SET.hasListeners();
    }

    public static IBlockState fireGet(Chunk c, int x, int y, int z) {
        return GET.firstNonNull(l -> l.onGetBlockState(c, x, y, z));
    }

    public static IBlockState fireSet(Chunk c, BlockPos p, IBlockState ns) {
        return SET.firstNonNull(l -> l.onSetBlockState(c, p, ns));
    }
}
