package com.hbm.events.interfaces;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public interface IWorldEventListener {
    /**
     * Called at head of {@link World#updateEntities()},
     */
    default void onUpdateEntities(World world) {
    }

    /**
     * Called before this.processingLoadedTiles = true, where all Entities are processed,
     * and TileEntity processing is about to start.
     */
    default void onTileEntityProcessingStart(World world) {
    }

    /**
     * Called after this.processingLoadedTiles = false, where all loaded TileEntities have been ticked,
     * and pending TileEntity processing is about to start.
     */
    default void onTileEntityProcessingEnd(World world) {
    }

    /**
     * Called at tail of {@link World#updateEntities()}
     */
    default void onUpdateEntitiesEnd(World world) {
    }

    /**
     * Called before {@code chunkProvider.tick()}
     */
    default void onUpdateChunkProvider(WorldServer server) {
    }

    /**
     * Called before {@link WorldServer#tickUpdates}
     */
    default void onUpdatePendingBlocks(WorldServer server) {
    }

    /**
     * Called before {@link WorldServer#updateBlocks}
     */
    default void onUpdateBlocks(WorldServer server) {
    }
}
