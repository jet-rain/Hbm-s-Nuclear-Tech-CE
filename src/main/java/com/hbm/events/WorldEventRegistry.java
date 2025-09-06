package com.hbm.events;

import com.hbm.events.interfaces.IWorldEventListener;
import com.hbm.interfaces.ServerThread;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

import static com.hbm.events.EventRegistry.overrides;

public final class WorldEventRegistry {

    private static final EventRegistry<IWorldEventListener> UPDATE_HEAD = EventRegistry.of(IWorldEventListener.class);
    private static final EventRegistry<IWorldEventListener> TILES_START = EventRegistry.of(IWorldEventListener.class);
    private static final EventRegistry<IWorldEventListener> TILES_END = EventRegistry.of(IWorldEventListener.class);
    private static final EventRegistry<IWorldEventListener> UPDATE_TAIL = EventRegistry.of(IWorldEventListener.class);
    private static final EventRegistry<IWorldEventListener> CHUNK_START = EventRegistry.of(IWorldEventListener.class);
    private static final EventRegistry<IWorldEventListener> PENDING_START = EventRegistry.of(IWorldEventListener.class);
    private static final EventRegistry<IWorldEventListener> BLOCK_START = EventRegistry.of(IWorldEventListener.class);

    @ApiStatus.Internal
    public static volatile boolean hasActiveListener = false;

    private WorldEventRegistry() {
    }

    @ServerThread
    public static void register(IWorldEventListener listener) {
        Objects.requireNonNull(listener, "listener");

        boolean onChunk = overrides(listener, IWorldEventListener.class, "onUpdateChunkProvider", WorldServer.class);
        boolean onPending = overrides(listener, IWorldEventListener.class, "onUpdatePendingBlocks", WorldServer.class);
        boolean onBlocks = overrides(listener, IWorldEventListener.class, "onUpdateBlocks", WorldServer.class);
        boolean onHead = overrides(listener, IWorldEventListener.class, "onUpdateEntities", World.class);
        boolean onStart = overrides(listener, IWorldEventListener.class, "onTileEntityProcessingStart", World.class);
        boolean onEnd = overrides(listener, IWorldEventListener.class, "onTileEntityProcessingEnd", World.class);
        boolean onTail = overrides(listener, IWorldEventListener.class, "onUpdateEntitiesEnd", World.class);

        if (onChunk) CHUNK_START.register(listener);
        if (onPending) PENDING_START.register(listener);
        if (onBlocks) BLOCK_START.register(listener);
        if (onHead) UPDATE_HEAD.register(listener);
        if (onStart) TILES_START.register(listener);
        if (onEnd) TILES_END.register(listener);
        if (onTail) UPDATE_TAIL.register(listener);

        hasActiveListener = anyActive();
    }

    @ServerThread
    public static void unregister(IWorldEventListener listener) {
        Objects.requireNonNull(listener, "listener");

        CHUNK_START.unregister(listener);
        PENDING_START.unregister(listener);
        BLOCK_START.unregister(listener);
        UPDATE_HEAD.unregister(listener);
        TILES_START.unregister(listener);
        TILES_END.unregister(listener);
        UPDATE_TAIL.unregister(listener);

        hasActiveListener = anyActive();
    }

    public static void fireChunkStart(WorldServer server) {
        if (!hasActiveListener) return;
        CHUNK_START.forEach(l -> l.onUpdateChunkProvider(server));
    }

    public static void firePendingBlocksStart(WorldServer server) {
        if (!hasActiveListener) return;
        PENDING_START.forEach(l -> l.onUpdatePendingBlocks(server));
    }

    public static void fireBlocksStart(WorldServer server) {
        if (!hasActiveListener) return;
        BLOCK_START.forEach(l -> l.onUpdateBlocks(server));
    }

    public static void fireUpdateHead(World world) {
        if (!hasActiveListener) return;
        UPDATE_HEAD.forEach(l -> l.onUpdateEntities(world));
    }

    public static void fireTilesStart(World world) {
        if (!hasActiveListener) return;
        TILES_START.forEach(l -> l.onTileEntityProcessingStart(world));
    }

    public static void fireTilesEnd(World world) {
        if (!hasActiveListener) return;
        TILES_END.forEach(l -> l.onTileEntityProcessingEnd(world));
    }

    public static void fireUpdateTail(World world) {
        if (!hasActiveListener) return;
        UPDATE_TAIL.forEach(l -> l.onUpdateEntitiesEnd(world));
    }

    private static boolean anyActive() {
        return UPDATE_HEAD.hasListeners() || TILES_START.hasListeners() || TILES_END.hasListeners() || UPDATE_TAIL.hasListeners() ||
               CHUNK_START.hasListeners() || PENDING_START.hasListeners() || BLOCK_START.hasListeners();
    }
}
