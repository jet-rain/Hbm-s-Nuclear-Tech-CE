package com.hbm.util;

import com.hbm.config.GeneralConfig;
import com.hbm.interfaces.ServerThread;
import com.hbm.interfaces.ThreadSafeMethod;
import com.hbm.lib.Library;
import com.hbm.lib.RefStrings;
import com.hbm.lib.UnsafeHolder;
import com.hbm.lib.maps.NonBlockingHashMapLong;
import com.hbm.main.MainRegistry;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BitArray;
import net.minecraft.util.IntIdentityHashBiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

import static com.hbm.lib.UnsafeHolder.U;

/**
 * Collection of high performance non-blocking chunk utilities
 *
 * @author mlbv
 */
@Mod.EventBusSubscriber(modid = RefStrings.MODID)
public final class ChunkUtil {

    private static final long ARR_BASE = U.arrayBaseOffset(ExtendedBlockStorage[].class);
    private static final long ARR_SCALE = U.arrayIndexScale(ExtendedBlockStorage[].class);
    private static final long UNLOAD_QUEUED_OFFSET = UnsafeHolder.fieldOffset(Chunk.class, "unloadQueued", "field_189550_d");
    private static final long BSL_STATES_OFFSET = UnsafeHolder.fieldOffset(BlockStatePaletteLinear.class, "states", "field_186042_a");
    private static final long BSL_ARRAY_SIZE_OFFSET = UnsafeHolder.fieldOffset(BlockStatePaletteLinear.class, "arraySize", "field_186045_d");
    private static final long BSHM_MAP_OFFSET = UnsafeHolder.fieldOffset(BlockStatePaletteHashMap.class, "statePaletteMap", "field_186046_a");
    private static final long IIHBM_VALUES_OFFSET = UnsafeHolder.fieldOffset(IntIdentityHashBiMap.class, "values", "field_186818_b");
    private static final long IIHBM_INTKEYS_OFFSET = UnsafeHolder.fieldOffset(IntIdentityHashBiMap.class, "intKeys", "field_186819_c");
    private static final long IIHBM_BYID_OFFSET = UnsafeHolder.fieldOffset(IntIdentityHashBiMap.class, "byId", "field_186820_d");
    private static final long IIHBM_NEXTFREE_OFFSET = UnsafeHolder.fieldOffset(IntIdentityHashBiMap.class, "nextFreeIndex", "field_186821_e");
    private static final long IIHBM_MAPSIZE_OFFSET = UnsafeHolder.fieldOffset(IntIdentityHashBiMap.class, "mapSize", "field_186822_f");
    private static final long TE_INVALID_OFFSET = UnsafeHolder.fieldOffset(TileEntity.class, "tileEntityInvalid", "field_145846_f");

    private static final ThreadLocal<Int2ObjectOpenHashMap<IBlockState>[]> TL_BUCKET = ThreadLocal.withInitial(() -> {
        // noinspection unchecked
        Int2ObjectOpenHashMap<IBlockState>[] maps = new Int2ObjectOpenHashMap[16];
        for (int i = 0; i < maps.length; i++) {
            maps[i] = new Int2ObjectOpenHashMap<>();
        }
        return maps;
    });
    private static final ThreadLocal<Long2ObjectOpenHashMap<IBlockState>> TL_SCRATCH = ThreadLocal.withInitial(Long2ObjectOpenHashMap::new);
    private static final IBlockState AIR_DEFAULT_STATE = Blocks.AIR.getDefaultState();
    private static final Int2IntOpenHashMap activeTask = new Int2IntOpenHashMap();
    private static final NonBlockingHashMapLong<NonBlockingHashMapLong<Chunk>> chunkMap = new NonBlockingHashMapLong<>();
    private static final int DENSE_THRESHOLD = 4096 / 3;

    private static int refCounter = 0;

    @ServerThread
    public static void acquireMirrorMap(WorldServer world) {
        int key = world.provider.getDimension();
        NonBlockingHashMapLong<Chunk> thisDim = new NonBlockingHashMapLong<>(4096); // half the initial capacity of loadedChunks
        if (activeTask.addTo(key, 1) == 0) {
            // This parallelStream is safe because the server thread is blocked at this time
            world.getChunkProvider().loadedChunks.values().parallelStream().forEach(chunk -> thisDim.put(ChunkPos.asLong(chunk.x, chunk.z), chunk));
            chunkMap.put(key, thisDim);
        }
        refCounter++;
        if (GeneralConfig.enableExtendedLogging) {
            MainRegistry.logger.info(
                    "Acquired mirror map for dimension {}. Active tasks of this dim = {}, reference count = {}.\nAll active dimensions: {}", key,
                    activeTask.get(key), refCounter, chunkMap.keySetLong());
        }
    }

    @ServerThread
    public static void releaseMirrorMap(WorldServer world) {
        int key = world.provider.getDimension();
        if (activeTask.addTo(key, -1) == 1) chunkMap.remove(key);
        refCounter--;
        if (GeneralConfig.enableExtendedLogging) {
            MainRegistry.logger.info(
                    "Released mirror map for dimension {}. Active tasks of this dim = {}, reference count = {}.\nAll active dimensions: {}", key,
                    activeTask.get(key), refCounter, chunkMap.keySetLong());
        }
    }

    @Nullable
    @ThreadSafeMethod
    public static Chunk getLoadedChunk(WorldServer world, long chunkPos) {
        int key = world.provider.getDimension();
        NonBlockingHashMapLong<Chunk> dimMap = chunkMap.get(key);
        if (dimMap == null) return null;
        Chunk chunk = dimMap.get(chunkPos);
        if (chunk == null) return null;
        U.putBooleanVolatile(chunk, UNLOAD_QUEUED_OFFSET, false);
        return chunk;
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (refCounter == 0) return;
        Chunk chunk = event.getChunk();
        int key = chunk.getWorld().provider.getDimension();
        if (activeTask.get(key) == 0) return;
        NonBlockingHashMapLong<Chunk> dimMap = chunkMap.get(key);
        if (dimMap != null) dimMap.put(ChunkPos.asLong(chunk.x, chunk.z), chunk);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (refCounter == 0) return;
        Chunk chunk = event.getChunk();
        int key = chunk.getWorld().provider.getDimension();
        NonBlockingHashMapLong<Chunk> dimMap = chunkMap.get(key);
        if (dimMap != null) dimMap.remove(ChunkPos.asLong(chunk.x, chunk.z));
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        int key = event.getWorld().provider.getDimension();
        activeTask.put(key, 0);
        chunkMap.remove(key);
    }

    /**
     * @return a deep copy of the given {@link BlockStateContainer}
     */
    @NotNull
    @ThreadSafeMethod
    @Contract("_ -> new")
    public static BlockStateContainer copyOf(@NotNull BlockStateContainer srcData) {
        final int bits = srcData.bits;
        final IBlockStatePalette srcPalette = srcData.palette;
        final BlockStateContainer copied = new BlockStateContainer();
        copied.bits = bits;

        if (bits <= 4) {
            copied.palette = new BlockStatePaletteLinear(bits, copied);
            final int arraySize = U.getInt(srcPalette, BSL_ARRAY_SIZE_OFFSET);
            U.putInt(copied.palette, BSL_ARRAY_SIZE_OFFSET, arraySize);
            final IBlockState[] srcStates = (IBlockState[]) U.getObject(srcPalette, BSL_STATES_OFFSET);
            final IBlockState[] dstStates = (IBlockState[]) U.getObject(copied.palette, BSL_STATES_OFFSET);
            System.arraycopy(srcStates, 0, dstStates, 0, arraySize);
        } else if (bits <= 8) {
            copied.palette = new BlockStatePaletteHashMap(bits, copied);
            final Object srcMap = U.getObject(srcPalette, BSHM_MAP_OFFSET);
            final Object dstMap = U.getObject(copied.palette, BSHM_MAP_OFFSET);

            final int nextFree = U.getInt(srcMap, IIHBM_NEXTFREE_OFFSET);
            final int mapSize = U.getInt(srcMap, IIHBM_MAPSIZE_OFFSET);
            U.putInt(dstMap, IIHBM_NEXTFREE_OFFSET, nextFree);
            U.putInt(dstMap, IIHBM_MAPSIZE_OFFSET, mapSize);

            final Object[] srcValues = (Object[]) U.getObject(srcMap, IIHBM_VALUES_OFFSET);
            final int[] srcIntKeys = (int[]) U.getObject(srcMap, IIHBM_INTKEYS_OFFSET);
            final Object[] srcById = (Object[]) U.getObject(srcMap, IIHBM_BYID_OFFSET);

            U.putObject(dstMap, IIHBM_VALUES_OFFSET, srcValues.clone());
            U.putObject(dstMap, IIHBM_INTKEYS_OFFSET, srcIntKeys.clone());
            U.putObject(dstMap, IIHBM_BYID_OFFSET, srcById.clone());
        } else {
            copied.palette = BlockStateContainer.REGISTRY_BASED_PALETTE;
        }

        final BitArray srcStorage = srcData.storage;
        copied.storage = new BitArray(bits, 4096);
        final long[] srcLongs = srcStorage.getBackingLongArray();
        final long[] dstLongs = copied.storage.getBackingLongArray();
        System.arraycopy(srcLongs, 0, dstLongs, 0, srcLongs.length);
        return copied;
    }

    /**
     * @return null when src is null or empty
     */
    @Nullable
    @ThreadSafeMethod
    @Contract(mutates = "param7, param8") // teRemovals and edgeOut
    public static ExtendedBlockStorage copyAndCarve(@NotNull WorldServer world, int chunkX, int chunkZ, int subY, ExtendedBlockStorage[] srcs,
                                                    ConcurrentBitSet bs, LongCollection teRemovals, LongCollection edgeOut) {
        ExtendedBlockStorage src = srcs[subY];
        if (src == Chunk.NULL_BLOCK_STORAGE || src.isEmpty()) return Chunk.NULL_BLOCK_STORAGE;
        final boolean hasSky = world.provider.hasSkyLight();
        final int height = world.getHeight();
        final ExtendedBlockStorage dst = new ExtendedBlockStorage(src.getYLocation(), hasSky);
        copyEBS(hasSky, src, dst);
        ExtendedBlockStorage[] storagesNegX = null, storagesPosX = null, storagesNegZ = null, storagesPosZ = null;
        final int startBit = (height - 1 - ((subY << 4) + 15)) << 8;
        final int endBit = ((height - 1 - (subY << 4)) << 8) | 0xFF;
        int bit = bs.nextSetBit(startBit);
        while (bit >= 0 && bit <= endBit) {
            final int yGlobal = height - 1 - (bit >>> 8);
            final int xGlobal = (chunkX << 4) | ((bit >>> 4) & 0xF);
            final int zGlobal = (chunkZ << 4) | (bit & 0xF);

            final int xLocal = xGlobal & 0xF;
            final int yLocal = yGlobal & 0xF;
            final int zLocal = zGlobal & 0xF;

            final IBlockState old = dst.get(xLocal, yLocal, zLocal);
            final Block oldBlock = old.getBlock();
            if (oldBlock != Blocks.AIR) {
                final long packed = Library.blockPosToLong(xGlobal, yGlobal, zGlobal);
                if (oldBlock.hasTileEntity(old)) teRemovals.add(packed);
                boolean touchesOutsideNonAir = false;
                if (yLocal == 0 && subY > 0) {
                    ExtendedBlockStorage below = srcs[subY - 1];
                    if (below != Chunk.NULL_BLOCK_STORAGE && !below.isEmpty()) {
                        IBlockState nb = below.get(xLocal, 15, zLocal);
                        if (nb.getBlock() != Blocks.AIR) touchesOutsideNonAir = true;
                    }
                }
                if (!touchesOutsideNonAir && yLocal == 15 && subY < (height >> 4) - 1) {
                    ExtendedBlockStorage above = srcs[subY + 1];
                    if (above != Chunk.NULL_BLOCK_STORAGE && !above.isEmpty()) {
                        IBlockState nb = above.get(xLocal, 0, zLocal);
                        if (nb.getBlock() != Blocks.AIR) touchesOutsideNonAir = true;
                    }
                }
                if (!touchesOutsideNonAir && xLocal == 0) {
                    int nCX = chunkX - 1;
                    if (storagesNegX == null) storagesNegX = getLoadedEBS(world, ChunkPos.asLong(nCX, chunkZ));
                    if (storagesNegX != null) {
                        ExtendedBlockStorage n = storagesNegX[subY];
                        if (n != Chunk.NULL_BLOCK_STORAGE && !n.isEmpty()) {
                            IBlockState nb = n.get(15, yLocal, zLocal);
                            if (nb.getBlock() != Blocks.AIR) touchesOutsideNonAir = true;
                        }
                    }
                }
                if (!touchesOutsideNonAir && xLocal == 15) {
                    int nCX = chunkX + 1;
                    if (storagesPosX == null) storagesPosX = getLoadedEBS(world, ChunkPos.asLong(nCX, chunkZ));
                    if (storagesPosX != null) {
                        ExtendedBlockStorage n = storagesPosX[subY];
                        if (n != Chunk.NULL_BLOCK_STORAGE && !n.isEmpty()) {
                            IBlockState nb = n.get(0, yLocal, zLocal);
                            if (nb.getBlock() != Blocks.AIR) touchesOutsideNonAir = true;
                        }
                    }
                }
                if (!touchesOutsideNonAir && zLocal == 0) {
                    int nCZ = chunkZ - 1;
                    if (storagesNegZ == null) storagesNegZ = getLoadedEBS(world, ChunkPos.asLong(chunkX, nCZ));
                    if (storagesNegZ != null) {
                        ExtendedBlockStorage n = storagesNegZ[subY];
                        if (n != Chunk.NULL_BLOCK_STORAGE && !n.isEmpty()) {
                            IBlockState nb = n.get(xLocal, yLocal, 15);
                            if (nb.getBlock() != Blocks.AIR) touchesOutsideNonAir = true;
                        }
                    }
                }
                if (!touchesOutsideNonAir && zLocal == 15) {
                    int nCZ = chunkZ + 1;
                    if (storagesPosZ == null) storagesPosZ = getLoadedEBS(world, ChunkPos.asLong(chunkX, nCZ));
                    if (storagesPosZ != null) {
                        ExtendedBlockStorage n = storagesPosZ[subY];
                        if (n != Chunk.NULL_BLOCK_STORAGE && !n.isEmpty()) {
                            IBlockState nb = n.get(xLocal, yLocal, 0);
                            if (nb.getBlock() != Blocks.AIR) touchesOutsideNonAir = true;
                        }
                    }
                }
                if (touchesOutsideNonAir) {
                    edgeOut.add(packed);
                }
                dst.set(xLocal, yLocal, zLocal, AIR_DEFAULT_STATE); // updates ref counts
            }
            bit = bs.nextSetBit(bit + 1);
        }
        return dst;
    }

    @ThreadSafeMethod
    public static boolean casEbsAt(ExtendedBlockStorage expect, ExtendedBlockStorage update, ExtendedBlockStorage[] arr, int subY) {
        final long off = ARR_BASE + ((long) subY) * ARR_SCALE;
        return U.compareAndSwapObject(arr, off, expect, update);
    }

    @Nullable
    @ThreadSafeMethod
    public static ExtendedBlockStorage[] getLoadedEBS(WorldServer world, long chunkPos) {
        Chunk chunk = getLoadedChunk(world, chunkPos);
        if (chunk == null) return null;
        return chunk.getBlockStorageArray();
    }

    private static void copyEBS(boolean hasSky, ExtendedBlockStorage src, ExtendedBlockStorage dst) {
        dst.data = copyOf(src.getData());
        if (!(src instanceof SubChunkSnapshot)) {
            dst.blockLight = new NibbleArray(src.getBlockLight().getData().clone());
            dst.skyLight = hasSky ? new NibbleArray(src.getSkyLight().getData().clone()) : null;
        }
        dst.blockRefCount = src.blockRefCount;
        dst.tickRefCount = src.tickRefCount;
    }


    /**
     * Apply block state changes to a chunk in-place using copy-on-write + CAS per subchunk.
     *
     * @param chunk        The chunk to modify.
     * @param function     Produces desired mutations. Result must not contain null values.
     * @param oldStatesOut Optional sink mapping global packed positions to their OLD (pre-change) states.
     */
    @ThreadSafeMethod
    public static void applyAndSwap(@NotNull Chunk chunk, @NotNull Function<Chunk, Long2ObjectMap<IBlockState>> function,
                                    @Nullable Long2ObjectMap<IBlockState> oldStatesOut) {

        final Long2ObjectMap<IBlockState> newStates = function.apply(chunk);
        if (newStates == null || newStates.isEmpty()) return;

        final WorldServer world = (WorldServer) chunk.getWorld();
        final boolean hasSky = world.provider.hasSkyLight();
        final int height = world.getHeight();
        final int chunkX = chunk.x, chunkZ = chunk.z;
        final ExtendedBlockStorage[] arr = chunk.getBlockStorageArray();

        final Int2ObjectOpenHashMap<IBlockState>[] bySub = TL_BUCKET.get();
        for (Int2ObjectOpenHashMap<IBlockState> map : bySub) map.clear();

        // bucket updates per subY with local-packed indices
        for (Long2ObjectMap.Entry<IBlockState> e : newStates.long2ObjectEntrySet()) {
            final long p = e.getLongKey();
            final int x = Library.getBlockPosX(p);
            final int y = Library.getBlockPosY(p);
            final int z = Library.getBlockPosZ(p);
            if ((x >> 4) != chunkX || (z >> 4) != chunkZ) continue;
            if (y < 0 || y >= height) continue;

            final int subY = y >> 4;
            Int2ObjectOpenHashMap<IBlockState> b = bySub[subY];
            if (b == null) bySub[subY] = b = new Int2ObjectOpenHashMap<>();
            b.put(Library.packLocal(x & 15, y & 15, z & 15), e.getValue());
        }

        final Long2ObjectOpenHashMap<IBlockState> subScratch = oldStatesOut == null ? null : TL_SCRATCH.get();

        for (int subY = 0; subY < 16; subY++) {
            if (subScratch != null) subScratch.clear();
            final Int2ObjectOpenHashMap<IBlockState> bucket = bySub[subY];
            if (bucket == null || bucket.isEmpty()) continue;

            if (bucket.size() < DENSE_THRESHOLD) {
                // sparse path
                while (true) {
                    final ExtendedBlockStorage src = arr[subY];
                    ExtendedBlockStorage working = null;
                    boolean any = false;

                    final int yBase = subY << 4;
                    final int xBase = chunkX << 4, zBase = chunkZ << 4;

                    for (Int2ObjectMap.Entry<IBlockState> e : bucket.int2ObjectEntrySet()) {
                        final int local = e.getIntKey();
                        final int lx = Library.unpackLocalX(local);
                        final int ly = Library.unpackLocalY(local);
                        final int lz = Library.unpackLocalZ(local);
                        final IBlockState ns = e.getValue();
                        if (ns == null) throw new NullPointerException("newState");

                        final IBlockState os = (src != Chunk.NULL_BLOCK_STORAGE && !src.isEmpty()) ? src.get(lx, ly, lz) : AIR_DEFAULT_STATE;
                        if (os == ns) continue;

                        if (working == null) {
                            if (src != Chunk.NULL_BLOCK_STORAGE && !src.isEmpty()) {
                                working = new ExtendedBlockStorage(src.getYLocation(), hasSky);
                                copyEBS(hasSky, src, working);
                            } else {
                                if (ns.getBlock() == Blocks.AIR) continue;
                                working = new ExtendedBlockStorage(yBase, hasSky);
                            }
                        }

                        working.set(lx, ly, lz, ns);
                        any = true;

                        if (subScratch != null) {
                            final long gpos = Library.blockPosToLong(xBase | lx, yBase | ly, zBase | lz);
                            subScratch.put(gpos, os);
                        }
                    }

                    if (!any) break;
                    final ExtendedBlockStorage update = working.isEmpty() ? Chunk.NULL_BLOCK_STORAGE : working;
                    if (casEbsAt(src, update, arr, subY)) {
                        if (oldStatesOut != null) oldStatesOut.putAll(subScratch);
                        break;
                    } else if (subScratch != null) {
                        subScratch.clear();
                    }
                }
            } else {
                // Dense path: build a 4096 override array, then sweep 0..4095 using indexTo*
                final IBlockState[] overrides = new IBlockState[4096];
                for (Int2ObjectMap.Entry<IBlockState> e : bucket.int2ObjectEntrySet()) {
                    overrides[e.getIntKey()] = e.getValue();
                }

                while (true) {
                    final ExtendedBlockStorage src = arr[subY];
                    ExtendedBlockStorage working = null;
                    boolean any = false;

                    for (int idx = 0; idx < 4096; idx++) {
                        final IBlockState newState = overrides[idx];
                        if (newState == null) continue;

                        final int lx = idx & 15;
                        final int ly = idx >>> 8;
                        final int lz = (idx >>> 4) & 15;

                        final IBlockState oldState = src == Chunk.NULL_BLOCK_STORAGE || src.isEmpty() ? AIR_DEFAULT_STATE : src.get(lx, ly, lz);
                        if (oldState == newState) continue;

                        if (working == null) {
                            if (src == Chunk.NULL_BLOCK_STORAGE || src.isEmpty()) {
                                working = new ExtendedBlockStorage(subY << 4, hasSky);
                            } else {
                                working = new ExtendedBlockStorage(src.getYLocation(), hasSky);
                                copyEBS(hasSky, src, working);
                            }
                        }

                        working.set(lx, ly, lz, newState);
                        any = true;

                        if (subScratch != null) {
                            final int x = indexToX(idx, chunkX);
                            final int y = indexToY(idx) | (subY << 4);
                            final int z = indexToZ(idx, chunkZ);
                            subScratch.put(Library.blockPosToLong(x, y, z), oldState);
                        }
                    }

                    if (!any) break;
                    final ExtendedBlockStorage update = working.isEmpty() ? Chunk.NULL_BLOCK_STORAGE : working;
                    if (casEbsAt(src, update, arr, subY)) {
                        if (oldStatesOut != null) oldStatesOut.putAll(subScratch);
                        break;
                    } else if (subScratch != null) {
                        subScratch.clear();
                    }
                }
            }
        }
    }

    /**
     * Return a modified copy of a subChunk.
     *
     * @param toUpdate     Map of <b>packed local</b> (x | y<<4 | z<<8) → new state. Values must be notnull.
     * @param oldStatesOut Optional sink mapping global packed block positions to their OLD (pre-change) states.
     * @return The modified copy of the subchunk's EBS, or null if no change.
     */
    @Nullable
    @ThreadSafeMethod
    public static ExtendedBlockStorage copyAndModify(int chunkX, int chunkZ, int subY, boolean hasSky, @Nullable ExtendedBlockStorage src,
                                                     @NotNull Int2ObjectMap<IBlockState> toUpdate,
                                                     @Nullable Long2ObjectMap<IBlockState> oldStatesOut) {
        if (toUpdate.isEmpty()) return null;

        ExtendedBlockStorage dst = null;
        boolean anyChange = false;
        final int xBase = chunkX << 4;
        final int yBase = subY << 4;
        final int zBase = chunkZ << 4;

        for (Int2ObjectMap.Entry<IBlockState> e : toUpdate.int2ObjectEntrySet()) {
            final int packedLocal = e.getIntKey();
            final int lx = Library.unpackLocalX(packedLocal);
            final int ly = Library.unpackLocalY(packedLocal);
            final int lz = Library.unpackLocalZ(packedLocal);

            final IBlockState newState = e.getValue();
            if (newState == null) throw new NullPointerException("newState");

            final IBlockState oldState = (src != Chunk.NULL_BLOCK_STORAGE && !src.isEmpty()) ? src.get(lx, ly, lz) : AIR_DEFAULT_STATE;
            if (oldState == newState) continue;

            if (dst == null) {
                if (src != Chunk.NULL_BLOCK_STORAGE && !src.isEmpty()) {
                    dst = new ExtendedBlockStorage(src.getYLocation(), hasSky);
                    copyEBS(hasSky, src, dst);
                } else {
                    if (newState.getBlock() == Blocks.AIR) continue;
                    dst = new ExtendedBlockStorage(yBase, hasSky);
                }
            }

            // record OLD state before change
            if (oldStatesOut != null) {
                final int xGlobal = xBase | lx;
                final int yGlobal = yBase | ly;
                final int zGlobal = zBase | lz;
                oldStatesOut.put(Library.blockPosToLong(xGlobal, yGlobal, zGlobal), oldState);
            }

            dst.set(lx, ly, lz, newState);
            anyChange = true;
        }

        if (!anyChange) return null;
        return dst.isEmpty() ? Chunk.NULL_BLOCK_STORAGE : dst;
    }

    @ServerThread
    public static void flushTileEntity(Chunk chunk, BlockPos pos, IBlockState oldState, IBlockState newState) {
        World world = chunk.getWorld();
        Block oldBlock = oldState.getBlock();
        Block newBlock = newState.getBlock();
        if (oldBlock != newBlock) oldBlock.breakBlock(world, pos, oldState);
        TileEntity te = chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
        if (te != null && te.shouldRefresh(world, pos, oldState, newState)) world.removeTileEntity(pos);
        Block block = newState.getBlock();
        if (!block.hasTileEntity(newState)) return;
        TileEntity newTileEntity = chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
        if (newTileEntity == null) {
            newTileEntity = block.createTileEntity(world, newState);
            if (newTileEntity != null) world.setTileEntity(pos, newTileEntity);
        }
        if (newTileEntity != null) newTileEntity.updateContainingBlockInfo();
    }

    @ThreadSafeMethod
    public static void invalidateTE(TileEntity te) {
        U.putBooleanVolatile(te, TE_INVALID_OFFSET, true);
    }

    @Contract(pure = true)
    public static int getChunkPosX(long chunkKey) {
        return (int) (chunkKey & 0xFFFFFFFFL);
    }

    @Contract(pure = true)
    public static int getChunkPosZ(long chunkKey) {
        return (int) ((chunkKey >>> 32) & 0xFFFFFFFFL);
    }

    @Contract(pure = true)
    public static int indexToX(int index, int chunkX) {
        return (chunkX << 4) | (index & 15);
    }

    @Contract(pure = true)
    public static int indexToY(int index) {
        return index >>> 8;
    }

    @Contract(pure = true)
    public static int indexToZ(int index, int chunkZ) {
        return (chunkZ << 4) | ((index >>> 4) & 15);
    }

    @Contract(pure = true)
    public static int indexToX(int index, long chunkPos) {
        return indexToX(index, getChunkPosX(chunkPos));
    }

    @Contract(pure = true)
    public static int indexToZ(int index, long chunkPos) {
        return indexToZ(index, getChunkPosZ(chunkPos));
    }

    @Contract(pure = true)
    public static long indexToBlockPos(int index, long chunkPos) {
        return Library.blockPosToLong((indexToX(index, chunkPos)), indexToY(index), indexToZ(index, chunkPos));
    }
}
