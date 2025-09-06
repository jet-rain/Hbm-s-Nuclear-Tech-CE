package com.hbm.core;

import com.hbm.events.WorldEventRegistry;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

@SuppressWarnings("unused")
public final class WorldHook {

    private WorldHook() {
    }

    public static void onUpdateChunkProvider(WorldServer server) {
        WorldEventRegistry.fireChunkStart(server);
    }

    public static void onUpdatePendingBlocks(WorldServer server) {
        WorldEventRegistry.firePendingBlocksStart(server);
    }

    public static void onUpdateBlocks(WorldServer server) {
        WorldEventRegistry.fireBlocksStart(server);
    }

    public static void onUpdateEntities(World world) {
        WorldEventRegistry.fireUpdateHead(world);
    }

    public static void onTileEntityProcessingStart(World world) {
        WorldEventRegistry.fireTilesStart(world);
    }

    public static void onTileEntityProcessingEnd(World world) {
        WorldEventRegistry.fireTilesEnd(world);
    }

    public static void onUpdateEntitiesEnd(World world) {
        WorldEventRegistry.fireUpdateTail(world);
    }
}
