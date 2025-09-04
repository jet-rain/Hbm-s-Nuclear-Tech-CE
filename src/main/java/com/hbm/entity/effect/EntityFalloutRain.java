package com.hbm.entity.effect;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.BombConfig;
import com.hbm.config.CompatibilityConfig;
import com.hbm.config.FalloutConfigJSON;
import com.hbm.config.FalloutConfigJSON.FalloutEntry;
import com.hbm.config.WorldConfig;
import com.hbm.entity.logic.EntityExplosionChunkloading;
import com.hbm.interfaces.AutoRegister;
import com.hbm.util.ChunkUtil;
import com.hbm.world.WorldUtil;
import com.hbm.world.biome.BiomeGenCraterBase;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.*;

@AutoRegister(name = "entity_fallout_rain", trackingRange = 1000)
public class EntityFalloutRain extends EntityExplosionChunkloading {

    private static final DataParameter<Integer> SCALE = EntityDataManager.createKey(EntityFalloutRain.class, DataSerializers.VARINT);
    private final List<Long> chunksToProcess = new ArrayList<>();
    private final List<Long> outerChunksToProcess = new ArrayList<>();
    private int tickDelay = BombConfig.falloutDelay;
    public UUID detonator;

    public EntityFalloutRain(World worldIn) {
        super(worldIn);
        this.setSize(4.0F, 20.0F);
        this.ignoreFrustumCheck = true;
        this.isImmuneToFire = true;
    }

    public EntityFalloutRain(World worldIn, int maxAge) {
        super(worldIn);
        this.setSize(4.0F, 20.0F);
        this.isImmuneToFire = true;
    }

    public static Biome getBiomeChange(double dist, int scale, Biome original) {
        if (!WorldConfig.enableCraterBiomes) return null;
        if (scale >= 150 && dist < 15) return BiomeGenCraterBase.craterInnerBiome;
        if (scale >= 100 && dist < 55 && original != BiomeGenCraterBase.craterInnerBiome) return BiomeGenCraterBase.craterBiome;
        if (scale >= 25 && original != BiomeGenCraterBase.craterInnerBiome && original != BiomeGenCraterBase.craterBiome)
            return BiomeGenCraterBase.craterOuterBiome;
        return null;
    }

    private static void addAllFromPairs(List<Long> out, int[] data) {
        if (data == null || data.length == 0) return;
        for (int i = 0; i + 1 < data.length; i += 2) {
            out.add(ChunkPos.asLong(data[i], data[i + 1]));
        }
    }

    private static int[] toPairsArray(List<Long> coords) {
        int[] data = new int[coords.size() * 2];
        for (int i = 0; i < coords.size(); i++) {
            long packed = coords.get(i);
            data[i * 2] = ChunkUtil.getChunkPosX(packed);
            data[i * 2 + 1] = ChunkUtil.getChunkPosZ(packed);
        }
        return data;
    }

    @Override
    public void onUpdate() {
        if (!world.isRemote) {
            long start = System.currentTimeMillis();
            if (!CompatibilityConfig.isWarDim(world)) this.setDead();
            else if (firstUpdate && chunksToProcess.isEmpty() && outerChunksToProcess.isEmpty()) gatherChunks();
            if (tickDelay == 0) {
                tickDelay = BombConfig.falloutDelay;

                while (System.currentTimeMillis() < start + BombConfig.mk5) {
                    if (!chunksToProcess.isEmpty()) {
                        long chunkPosLong = chunksToProcess.remove(chunksToProcess.size() - 1);
                        int chunkPosX = ChunkUtil.getChunkPosX(chunkPosLong);
                        int chunkPosZ = ChunkUtil.getChunkPosZ(chunkPosLong);
                        boolean biomeModified = false;
                        for (int x = (chunkPosX << 4); x < (chunkPosX << 4) + 16; x++) {
                            for (int z = (chunkPosZ << 4); z < (chunkPosZ << 4) + 16; z++) {
                                double percent = Math.hypot(x - this.posX, z - this.posZ) * 100.0 / getScale();
                                stomp(x, z, percent);
                                Biome biome = getBiomeChange(percent, getScale(), world.getBiome(new BlockPos(x, 0, z)));
                                if (biome != null) {
                                    WorldUtil.setBiome(world, x, z, biome);
                                    biomeModified = true;
                                }
                            }
                        }
                        if (biomeModified) WorldUtil.syncBiomeChange(world, chunkPosX, chunkPosZ);
                    } else if (!outerChunksToProcess.isEmpty()) {
                        long chunkPosLong = outerChunksToProcess.remove(outerChunksToProcess.size() - 1);
                        int chunkPosX = ChunkUtil.getChunkPosX(chunkPosLong);
                        int chunkPosZ = ChunkUtil.getChunkPosZ(chunkPosLong);
                        boolean biomeModified = false;
                        for (int x = (chunkPosX << 4); x < (chunkPosX << 4) + 16; x++) {
                            for (int z = (chunkPosZ << 4); z < (chunkPosZ << 4) + 16; z++) {
                                double distance = Math.hypot(x - this.posX, z - this.posZ);
                                if (distance <= getScale()) {
                                    double percent = distance * 100.0 / getScale();
                                    stomp(x, z, percent);
                                    Biome biome = getBiomeChange(percent, getScale(), world.getBiome(new BlockPos(x, 0, z)));
                                    if (biome != null) {
                                        WorldUtil.setBiome(world, x, z, biome);
                                        biomeModified = true;
                                    }
                                }
                            }
                        }
                        if (biomeModified) WorldUtil.syncBiomeChange(world, chunkPosX, chunkPosZ);
                    } else {
                        this.clearChunkLoader();
                        this.setDead();
                        break;
                    }
                }
            }
            tickDelay--;
        }
        super.onUpdate();
    }

    // Is it worth the effort to split this into a method that can be called over multiple ticks? I'd say it's fast enough anyway...
    private void gatherChunks() {
        Set<Long> chunks = new LinkedHashSet<>();
        Set<Long> outerChunks = new LinkedHashSet<>();
        int outerRange = getScale();

        // step size heuristic (minimum ~18 to cover all chunks)
        int adjustedMaxAngle = 20 * outerRange / 32;
        if (adjustedMaxAngle < 18) adjustedMaxAngle = 18;

        // outer ring
        for (int angle = 0; angle <= adjustedMaxAngle; angle++) {
            double theta = angle * (2.0 * Math.PI) / adjustedMaxAngle; // full rotation
            Vec3d vec = new Vec3d(outerRange, 0, 0).rotateYaw((float) theta);
            int cx = ((int) Math.floor(this.posX + vec.x)) >> 4;
            int cz = ((int) Math.floor(this.posZ + vec.z)) >> 4;
            outerChunks.add(ChunkPos.asLong(cx, cz));
        }

        // interior (radial spokes every 8 blocks)
        for (int distance = 0; distance <= outerRange; distance += 8) {
            for (int angle = 0; angle <= adjustedMaxAngle; angle++) {
                double theta = angle * (2.0 * Math.PI) / adjustedMaxAngle;
                Vec3d vec = new Vec3d(distance, 0, 0).rotateYaw((float) theta);
                int cx = ((int) Math.floor(this.posX + vec.x)) >> 4;
                int cz = ((int) Math.floor(this.posZ + vec.z)) >> 4;
                long packed = ChunkPos.asLong(cx, cz);
                if (!outerChunks.contains(packed)) chunks.add(packed);
            }
        }

        chunksToProcess.addAll(chunks);
        outerChunksToProcess.addAll(outerChunks);
        Collections.reverse(chunksToProcess); // start nicely from the middle
        Collections.reverse(outerChunksToProcess);
    }

    private void stomp(int x, int z, double dist) {
        int depth = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = 255; y >= 0; y--) {
            if (depth >= 3) return;

            pos.setPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            if (state.getMaterial() == Material.AIR || block == ModBlocks.fallout) continue;

            // TODO: implement volcano_rad_core
//			if (block == ModBlocks.volcano_core) {
//				world.setBlockState(pos, ModBlocks.volcano_rad_core.getDefaultState(), 3);
//				continue;
//			}

            BlockPos posUp = pos.up();
            IBlockState stateUp = world.getBlockState(posUp);

            if (depth == 0 && (world.isAirBlock(posUp) || stateUp.getBlock().isReplaceable(world, posUp) && !stateUp.getMaterial().isLiquid())) {

                double d = dist / 100.0;
                double chance = 0.1 - Math.pow((d - 0.7), 2.0);

                if (chance >= rand.nextDouble() && ModBlocks.fallout.canPlaceBlockAt(world, posUp)) {
                    world.setBlockState(posUp, ModBlocks.fallout.getDefaultState(), 3);
                }
            }

            if (dist < 65 && block.isFlammable(world, pos, EnumFacing.UP)) {
                if (rand.nextInt(5) == 0 && world.isAirBlock(posUp)) {
                    world.setBlockState(posUp, Blocks.FIRE.getDefaultState(), 3);
                }
            }

            boolean eval = false;

            for (FalloutEntry entry : FalloutConfigJSON.entries) {
                IBlockState result = entry.eval(pos.getY(), state, dist, rand);
                if (result != null) {
                    world.setBlockState(pos, result, 3);
                    if (entry.isSolid()) {
                        depth++;
                    }
                    eval = true;
                    break;
                }
            }

            float hardness = state.getBlockHardness(world, pos);
            if (y > 0 && dist < 65 && hardness <= Blocks.STONEBRICK.getExplosionResistance(null) && hardness >= 0) {
                BlockPos below = pos.down();
                if (world.isAirBlock(below)) {
                    for (int i = 0; i <= depth; i++) {
                        BlockPos p = pos.add(0, i, 0);
                        IBlockState s = world.getBlockState(p);
                        float h = s.getBlockHardness(world, p);
                        if (h <= Blocks.STONEBRICK.getExplosionResistance(null) && h >= 0) {
                            EntityFallingBlock falling = new EntityFallingBlock(world, x + 0.5D, y + 0.5D + i, z + 0.5D, s);
                            falling.shouldDropItem = false; // turn off block drops because block dropping was coded by a mule with dementia
                            world.spawnEntity(falling);
                        }
                    }
                }
            }

            if (!eval && state.isNormalCube()) {
                depth++;
            }
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(SCALE, 1);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        setScale(tag.getInteger("scale"));
        chunksToProcess.clear();
        outerChunksToProcess.clear();
        addAllFromPairs(chunksToProcess, tag.getIntArray("chunks"));
        addAllFromPairs(outerChunksToProcess, tag.getIntArray("outerChunks"));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setInteger("scale", getScale());
        tag.setIntArray("chunks", toPairsArray(chunksToProcess));
        tag.setIntArray("outerChunks", toPairsArray(outerChunksToProcess));
    }

    public int getScale() {
        Integer scale = this.dataManager.get(SCALE);
        return scale <= 0 ? 1 : scale;
    }

    public void setScale(int i) {
        this.dataManager.set(SCALE, i);
    }

    public void setScale(int i, int ignored) {
        this.dataManager.set(SCALE, i);
    }
}