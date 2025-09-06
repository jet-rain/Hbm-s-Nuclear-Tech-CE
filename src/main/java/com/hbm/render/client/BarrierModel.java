package com.hbm.render.client;

import com.hbm.lib.RefStrings;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@SideOnly(Side.CLIENT)
public class BarrierModel implements IBakedModel {
  @Override
  public List<BakedQuad> getQuads(
      @Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
    return Collections.emptyList();
  }

  @Override
  public boolean isAmbientOcclusion() {
    return false;
  }

  @Override
  public boolean isGui3d() {
    return false;
  }

  @Override
  public boolean isBuiltInRenderer() {
    return true;
  }

  @Override
  public TextureAtlasSprite getParticleTexture() {
      return Minecraft.getMinecraft()
              .getTextureMapBlocks()
              .getAtlasSprite(new ResourceLocation(RefStrings.MODID, "blocks/wood_barrier").toString());
  }

  @Override
  public ItemOverrideList getOverrides() {
    return ItemOverrideList.NONE;
  }
}
