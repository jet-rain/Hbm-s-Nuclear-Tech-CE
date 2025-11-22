package com.hbm.blocks.gas;

import biomesoplenty.api.block.BOPBlocks;
import biomesoplenty.common.block.BlockBOPGrass;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.handler.ArmorUtil;
import com.hbm.lib.ForgeDirection;
import com.hbm.potion.HbmPotion;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.EnumUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class BlockGasRadonTomb extends BlockGasBase {

    /*
     * You should not have come here.
     *
     * This is not a place of honor. No great deed is commemorated here.
     *
     * Nothing of value is here.
     *
     * What is here is dangerous and repulsive.
     *
     * We considered ourselves a powerful culture. We harnessed the hidden fire,
     * and used it for our own purposes.
     *
     * Then we saw the fire could burn within living things, unnoticed until it
     * destroyed them.
     *
     * And we were afraid.
     *
     * We built great tombs to hold the fire for one hundred thousand years,
     * after which it would no longer kill.
     *
     * If this place is opened, the fire will not be isolated from the world,
     * and we will have failed to protect you.
     *
     * Leave this place and never come back.
     */

    public BlockGasRadonTomb(String s) {
        super(0.1F, 0.3F, 0.1F, s);
    }

    @Override
    public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entity) {

        if (!GeneralConfig.enableRadon) {
            return;
        }

        if (entity instanceof EntityLivingBase) {


            EntityLivingBase entityLiving = (EntityLivingBase) entity;

            if (ArmorRegistry.hasProtection(entityLiving, EntityEquipmentSlot.HEAD, HazardClass.RAD_GAS)) {
                ArmorUtil.damageGasMaskFilter(entityLiving, 4);
                ContaminationUtil.contaminate(entityLiving, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
            } else {
                entityLiving.removePotionEffect(HbmPotion.radaway); //get fucked
                entityLiving.removePotionEffect(HbmPotion.radx);
                ContaminationUtil.contaminate(entityLiving, HazardType.RADIATION, ContaminationType.RAD_BYPASS, 5F);
            }
        }
    }

    @Override
    public ForgeDirection getFirstDirection(World world, int x, int y, int z) {

        if (world.rand.nextInt(3) == 0)
            return ForgeDirection.UP;

        return ForgeDirection.DOWN;
    }

    @Override
    public ForgeDirection getSecondDirection(World world, int x, int y, int z) {
        return this.randomHorizontal(world);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {

        if (!world.isRemote) {

            if (!GeneralConfig.enableRadon) {
                world.setBlockToAir(pos);
                return;
            }

            if (rand.nextInt(10) == 0) {
                IBlockState state2 = world.getBlockState(pos.down());
                Block b = state2.getBlock();

                if (b == Blocks.GRASS) {
                    if (rand.nextInt(5) == 0)
                        world.setBlockState(pos.down(), Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT), 3);
                    else
                        world.setBlockState(pos.down(), ModBlocks.waste_earth.getDefaultState());
                } else if (b == BOPBlocks.grass) {
                    BlockBOPGrass.BOPGrassType grassType = EnumUtil.grabEnumSafely(BlockBOPGrass.BOPGrassType.class, getMetaFromState(world.getBlockState(pos)));
                    switch (grassType) {
                        case SILTY:
                            world.setBlockState(pos.down(), ModBlocks.waste_earth_silty.getDefaultState());
                        case SANDY:
                            world.setBlockState(pos.down(), ModBlocks.waste_earth_sandy.getDefaultState());
                        case LOAMY:
                            world.setBlockState(pos.down(), ModBlocks.waste_earth_loamy.getDefaultState());
                        case DAISY:
                            world.setBlockState(pos.down(), ModBlocks.waste_earth_daisy.getDefaultState());
                        case OVERGROWN_STONE:
                            if (rand.nextInt(5) == 0)
                                world.setBlockState(pos.down(), Blocks.STONE.getDefaultState());
                            else
                                world.setBlockState(pos.down(), ModBlocks.waste_stone.getDefaultState());
                        case OVERGROWN_NETHERRACK:
                            if (rand.nextInt(5) == 0)
                                world.setBlockState(pos.down(), Blocks.NETHERRACK.getDefaultState());
                            else
                             world.setBlockState(pos.down(), ModBlocks.waste_stone_netherrack.getDefaultState());
                        case SPECTRAL_MOSS:
                            if (rand.nextInt(5) == 0)
                                world.setBlockState(pos.down(), Blocks.END_STONE.getDefaultState());
                            else
                                world.setBlockState(pos.down(), ModBlocks.waste_stone_moss.getDefaultState());
                        case ORIGIN:
                            if (rand.nextInt(5) == 0)
                                world.setBlockState(pos.down(), Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT), 3);
                            else
                                world.setBlockState(pos.down(), ModBlocks.waste_earth.getDefaultState());
                    }
                }
                if ((state2.getMaterial() == Material.GRASS || state2.getMaterial() == Material.LEAVES || state2.getMaterial() == Material.PLANTS || state2.getMaterial() == Material.VINE) && !state2.isNormalCube())
                    world.setBlockToAir(pos.down());
            }

            if (rand.nextInt(600) == 0) {
                world.setBlockToAir(pos);
                return;
            }
        }

        super.updateTick(world, pos, state, rand);
    }
}