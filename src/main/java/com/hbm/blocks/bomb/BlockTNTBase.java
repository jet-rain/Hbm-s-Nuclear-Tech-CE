package com.hbm.blocks.bomb;

import com.hbm.api.block.IToolable;
import com.hbm.entity.item.EntityTNTPrimedBase;
import com.hbm.render.block.BlockBakeFrame;
import com.hbm.util.ChatBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public abstract class BlockTNTBase extends BlockDetonatable implements IToolable {

    public BlockTNTBase(String s) {
        super(Material.TNT, s, 15, 100, 20, false, false);
        this.setDefaultState(this.blockState.getBaseState().withProperty(META, 0));
        this.META_COUNT = 1;
    }

    public BlockTNTBase(String s, BlockBakeFrame... frames) {
        super(Material.TNT, s, 15, 100, 20, false, false, frames);
        this.setDefaultState(this.blockState.getBaseState().withProperty(META, 0));
        this.META_COUNT = 1;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        if (world.isBlockPowered(pos)) {
            if (!world.isRemote) {
                this.prime(world, pos, state.withProperty(META, 1), null);
                world.setBlockToAir(pos);
            }
        } else {
            checkAndIgnite(world, pos);
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (world.isBlockPowered(pos)) {
            this.onPlayerDestroy(world, pos, state);
            world.setBlockToAir(pos);
        } else {
            checkAndIgnite(world, pos);
        }
    }

    private void checkAndIgnite(World world, BlockPos pos) {
        if (shouldIgnite(world, pos)) {
            IBlockState state = world.getBlockState(pos);
            this.onPlayerDestroy(world, pos, state);
            world.setBlockToAir(pos);
        }
    }

    @Override
    public void onPlayerDestroy(World world, BlockPos pos, IBlockState state) {
        this.prime(world, pos, state, null);
    }

    private void prime(World world, BlockPos pos, IBlockState state, EntityLivingBase living) {
        if (!world.isRemote) {
            if (state.getValue(META) == 1) {
                EntityTNTPrimedBase tnt = new EntityTNTPrimedBase(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, living, state);
                world.spawnEntity(tnt);
                world.playSound(null, tnt.posX, tnt.posY, tnt.posZ, net.minecraft.init.SoundEvents.ENTITY_TNT_PRIMED,
                        net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX,
                                    float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() == Items.FLINT_AND_STEEL) {
            IBlockState ignitingState = state.withProperty(META, 1);
            this.prime(world, pos, ignitingState, player);
            world.setBlockToAir(pos);
            held.damageItem(1, player);
            return true;
        }
        return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (!world.isRemote && entity instanceof EntityArrow arrow) {
            if (arrow.isBurning()) {
                this.prime(world, pos, state.withProperty(META, 1),
                        arrow.shootingEntity instanceof EntityLivingBase ? (EntityLivingBase) arrow.shootingEntity : null);
                world.setBlockToAir(pos);
            }
        }
    }

    @Override
    public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, EnumFacing side, float fX, float fY, float fZ, EnumHand hand,
                           ToolType tool) {

        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = world.getBlockState(pos);

        if (tool == ToolType.DEFUSER) {
            if (!world.isRemote) {
                world.destroyBlock(pos, false);
                this.dropBlockAsItem(world, pos, this.getDefaultState().withProperty(META, 0), 0);
            }
            return true;
        }

        if (tool != ToolType.SCREWDRIVER) return false;

        if (!world.isRemote) {
            int next = state.getValue(META) == 0 ? 1 : 0;
            world.setBlockState(pos, state.withProperty(META, next), 3);

            if (next == 1) {
                player.sendMessage(ChatBuilder.start("[ Ignite On Break: Enabled ]").color(TextFormatting.RED).flush());
            } else {
                player.sendMessage(ChatBuilder.start("[ Ignite On Break: Disabled ]").color(TextFormatting.GOLD).flush());
            }
        }
        return true;
    }
}
