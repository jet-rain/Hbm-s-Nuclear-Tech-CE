//package com.hbm.render.tileentity;
//
//import com.hbm.blocks.ModBlocks;
//import com.hbm.interfaces.AutoRegister;
//import com.hbm.lib.RefStrings;
//import com.hbm.render.item.ItemRenderBase;
//import com.hbm.tileentity.deco.TileEntityBarrier;
//import net.minecraft.block.state.IBlockState;
//import net.minecraft.client.renderer.BufferBuilder;
//import net.minecraft.client.renderer.GlStateManager;
//import net.minecraft.client.renderer.Tessellator;
//import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
//import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
//import net.minecraft.item.Item;
//import net.minecraft.util.EnumFacing;
//import net.minecraft.util.ResourceLocation;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.world.World;
//import org.lwjgl.opengl.GL11;
//
//@AutoRegister
//public class RenderBarrier extends TileEntitySpecialRenderer<TileEntityBarrier>
//    implements IItemRendererProvider {
//
//  public static final ResourceLocation TEXTURE =
//      new ResourceLocation(RefStrings.MODID + ":textures/blocks/wood_barrier.png");
//
//  @Override
//  public void render(
//      TileEntityBarrier te,
//      double x,
//      double y,
//      double z,
//      float partialTicks,
//      int destroyStage,
//      float alpha) {
//
//    GlStateManager.pushMatrix();
//    GlStateManager.translate(x, y, z);
//    GlStateManager.color(1.0F, 1.0F, 1.0F);
//    GlStateManager.enableLighting();
//
//    int meta = te.getBlockMetadata();
//    EnumFacing facing = EnumFacing.byHorizontalIndex(meta);
//
//    Tessellator tessellator = Tessellator.getInstance();
//    BufferBuilder buffer = tessellator.getBuffer();
//
//    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
//    bindTexture(TEXTURE);
//
//    BlockPos pos = te.getPos();
//    World world = te.getWorld();
//
//    IBlockState nx = world.getBlockState(pos.west());
//    IBlockState px = world.getBlockState(pos.east());
//    IBlockState nz = world.getBlockState(pos.north());
//    IBlockState pz = world.getBlockState(pos.south());
//    IBlockState py = world.getBlockState(pos.up());
//
//    boolean negX = nx.isOpaqueCube() || nx.isNormalCube() || facing == EnumFacing.EAST;
//    boolean negZ = nz.isOpaqueCube() || nz.isNormalCube() || facing == EnumFacing.SOUTH;
//    boolean posX = px.isOpaqueCube() || px.isNormalCube() || facing == EnumFacing.WEST;
//    boolean posZ = pz.isOpaqueCube() || pz.isNormalCube() || facing == EnumFacing.NORTH;
//    boolean posY = py.isOpaqueCube() || py.isNormalCube();
//
//    if (negX) {
//      renderCube(buffer, 0D, 0D, 0.4375D, 0.125D, 1D, 0.5625D);
//      renderCube(buffer, 0D, 0.0625D, negZ ? 0.125D : 0D, 0.0625D, 0.4375D, posZ ? 0.875D : 1D);
//      renderCube(buffer, 0D, 0.5625D, negZ ? 0.125D : 0D, 0.0625D, 0.9375D, posZ ? 0.875D : 1D);
//    }
//    if (negZ) {
//      renderCube(buffer, 0.4375D, 0D, 0D, 0.5625D, 1D, 0.125D);
//      renderCube(buffer, negX ? 0.125D : 0D, 0.0625D, 0D, posX ? 0.875D : 1D, 0.4375D, 0.0625D);
//      renderCube(buffer, negX ? 0.125D : 0D, 0.5625D, 0D, posX ? 0.875D : 1D, 0.9375D, 0.0625D);
//    }
//    if (posX) {
//      renderCube(buffer, 0.875D, 0D, 0.4375D, 1D, 1D, 0.5625D);
//      renderCube(buffer, 0.9375D, 0.0625D, negZ ? 0.125D : 0D, 1D, 0.4375D, posZ ? 0.875D : 1D);
//      renderCube(buffer, 0.9375D, 0.5625D, negZ ? 0.125D : 0D, 1D, 0.9375D, posZ ? 0.875D : 1D);
//    }
//    if (posZ) {
//      renderCube(buffer, 0.4375D, 0D, 0.875D, 0.5625D, 1D, 1D);
//      renderCube(buffer, negX ? 0.125D : 0D, 0.0625D, 0.9375D, posX ? 0.875D : 1D, 0.4375D, 1D);
//      renderCube(buffer, negX ? 0.125D : 0D, 0.5625D, 0.9375D, posX ? 0.875D : 1D, 0.9375D, 1D);
//    }
//    if (posY) {
//      renderCube(buffer, 0D, 0.875D, 0D, 0.125D, 0.9375D, 1D);
//      renderCube(buffer, 0.875D, 0.875D, 0D, 1D, 0.9375D, 1D);
//      renderCube(buffer, 0D, 0.9375D, 0.0625D, 1D, 1D, 0.4375D);
//      renderCube(buffer, 0D, 0.9375D, 0.5625D, 1D, 1D, 0.9375D);
//    }
//
//    tessellator.draw();
//
//    GlStateManager.disableLighting();
//    GlStateManager.popMatrix();
//  }
//
//  @Override
//  public Item getItemForRenderer() {
//    return Item.getItemFromBlock(ModBlocks.wood_barrier);
//  }
//
//  @Override
//  public ItemRenderBase getRenderer(Item item) {
//    return new ItemRenderBase() {
//      public void renderInventory() {
//        GlStateManager.disableLighting();
//        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
//        GlStateManager.translate(-0.5F, 0.0F, -0.5F);
//        GlStateManager.scale(4.0F, 4.0F, 4.0F);
//      }
//
//      public void renderCommon() {
//        GlStateManager.pushMatrix();
//        GlStateManager.disableLighting();
//        GlStateManager.color(1.0F, 1.0F, 1.0F);
//        GlStateManager.scale(2.0, 2.0, 2.0);
//
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder buffer = tessellator.getBuffer();
//
//        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
//        bindTexture(TEXTURE);
//
//        renderCube(buffer, 0.4375, 0D, 0.4375D, 0.5625D, 1D, 0.5625D);
//        renderCube(buffer, 0.5D, 0.0625D, 0D, 0.5625D, 0.4725, 1D);
//        renderCube(buffer, 0.5D, 0.5625D, 0D, 0.5625D, 0.9375, 1D);
//
//        tessellator.draw();
//
//        GlStateManager.popMatrix();
//      }
//    };
//  }
//
//  private void renderCube(
//      BufferBuilder buffer,
//      double minX,
//      double minY,
//      double minZ,
//      double maxX,
//      double maxY,
//      double maxZ) {
//
//    double width = maxX - minX;
//    double height = maxY - minY;
//    double depth = maxZ - minZ;
//
//    buffer.pos(minX, minY, minZ).tex(0, 0).endVertex();
//    buffer.pos(minX, maxY, minZ).tex(0, height).endVertex();
//    buffer.pos(maxX, maxY, minZ).tex(width, height).endVertex();
//    buffer.pos(maxX, minY, minZ).tex(width, 0).endVertex();
//
//    buffer.pos(maxX, minY, maxZ).tex(0, 0).endVertex();
//    buffer.pos(maxX, maxY, maxZ).tex(0, height).endVertex();
//    buffer.pos(minX, maxY, maxZ).tex(width, height).endVertex();
//    buffer.pos(minX, minY, maxZ).tex(width, 0).endVertex();
//
//    buffer.pos(minX, minY, maxZ).tex(0, 0).endVertex();
//    buffer.pos(minX, maxY, maxZ).tex(0, height).endVertex();
//    buffer.pos(minX, maxY, minZ).tex(depth, height).endVertex();
//    buffer.pos(minX, minY, minZ).tex(depth, 0).endVertex();
//
//    buffer.pos(maxX, minY, minZ).tex(0, 0).endVertex();
//    buffer.pos(maxX, maxY, minZ).tex(0, height).endVertex();
//    buffer.pos(maxX, maxY, maxZ).tex(depth, height).endVertex();
//    buffer.pos(maxX, minY, maxZ).tex(depth, 0).endVertex();
//
//    buffer.pos(minX, minY, minZ).tex(0, 0).endVertex();
//    buffer.pos(maxX, minY, minZ).tex(width, 0).endVertex();
//    buffer.pos(maxX, minY, maxZ).tex(width, depth).endVertex();
//    buffer.pos(minX, minY, maxZ).tex(0, depth).endVertex();
//
//    buffer.pos(minX, maxY, maxZ).tex(0, 0).endVertex();
//    buffer.pos(maxX, maxY, maxZ).tex(width, 0).endVertex();
//    buffer.pos(maxX, maxY, minZ).tex(width, depth).endVertex();
//    buffer.pos(minX, maxY, minZ).tex(0, depth).endVertex();
//  }
//
//  @Override
//  public boolean isGlobalRenderer(TileEntityBarrier te) {
//    return false;
//  }
//}
