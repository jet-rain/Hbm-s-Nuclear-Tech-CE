package com.hbm.blocks.bomb;
import com.hbm.blocks.BlockEnums;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.util.EnumUtil;
import com.hbm.blocks.BlockEnumMeta;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.IBomb;
import com.hbm.items.ModItems;
import com.hbm.tileentity.bomb.TileEntityCrashedBomb;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.List;

public class BlockCrashedBomb extends BlockEnumMeta implements IBomb {

    public static enum EnumDudType {
        BALEFIRE, CONVENTIONAL, NUKE, SALTED
    }

    public BlockCrashedBomb(Material mat, SoundType type, String registryName) {

        super(mat, type, registryName, EnumDudType.class, true, true);
    }


    public TileEntity createNewTileEntity(World worldIn, int meta) {

        return new TileEntityCrashedBomb();
    }



    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote)
            return true;
        Item tool = player.getHeldItem(hand).getItem();
        if (tool == ModItems.defuser || tool == ModItems.defuser_desh) {
            if (tool.getMaxDamage(player.getHeldItem(hand)) > 0)
                player.getHeldItem(hand).damageItem(1, player);


            EnumDudType type = EnumUtil.grabEnumSafely(EnumDudType.class, getMetaFromState(world.getBlockState(pos)));

            //TODO: make this less scummy
            if (type == type.BALEFIRE) {
                world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModItems.egg_balefire_shard)));
            }
            if (type == type.CONVENTIONAL) {
                world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModItems.ball_tnt, 16)));
            }
            if (type == type.NUKE) {
                world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModItems.ball_tnt, 8)));
                world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModItems.billet_plutonium, 4)));
            }
            if (type == type.SALTED) {
                world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModItems.ball_tnt, 8)));
                world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModItems.billet_plutonium, 2)));
                world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 + 0.5, new ItemStack(ModItems.ingot_cobalt, 12)));
            }
            world.destroyBlock(pos, false);
            return true;
        }
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isBlockNormalCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }





    @Override
    public BombReturnCode explode(World world, BlockPos pos, Entity detonator) {
        if (!world.isRemote) {
            int meta = this.blockState.getBaseState().getValue(META);
            EnumDudType type = EnumUtil.grabEnumSafely(EnumDudType.class, getMetaFromState(world.getBlockState(pos)));
            world.setBlockToAir(pos);

            if (type == type.BALEFIRE) {
                EntityBalefire bf = new EntityBalefire(world);
                bf.setPosition(pos.getX(),pos.getY(),pos.getZ());
                bf.destructionRange = (int) (BombConfig.fatmanRadius * 1.25);
                world.spawnEntity(bf);
                if(BombConfig.enableNukeClouds) {
                    EntityNukeTorex.statFacBale(world, pos.getX() + 0.5, pos.getY() + 5, pos.getZ() + 0.5, (int) (BombConfig.fatmanRadius * 1.25));
                }
            }

            if (type == type.CONVENTIONAL) {
                ExplosionVNT xnt = new ExplosionVNT(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 35F);
                xnt.setBlockAllocator(new BlockAllocatorStandard(24));
                xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
                xnt.setEntityProcessor(new EntityProcessorCross(5D).withRangeMod(1.5F));
                xnt.setPlayerProcessor(new PlayerProcessorStandard());
                xnt.explode();
                ExplosionCreator.composeEffectLarge(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            }

            if (type == type.NUKE) {
                world.spawnEntity(EntityNukeExplosionMK5.statFac(world, 35, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
                if(BombConfig.enableNukeClouds) {
                    EntityNukeTorex.statFac(world, pos.getX() + 0.5, pos.getY() + 5, pos.getZ() + 0.5, (int) (35));
                }
            }

            if (type == type.SALTED) {
                world.spawnEntity(EntityNukeExplosionMK5.statFac(world, 25, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).moreFallout(25));
                if(BombConfig.enableNukeClouds) {
                    EntityNukeTorex.statFac(world, pos.getX() + 0.5, pos.getY() + 5, pos.getZ() + 0.5, (int) (25));
                }
            }
        }

        return BombReturnCode.DETONATED;
    }
}
