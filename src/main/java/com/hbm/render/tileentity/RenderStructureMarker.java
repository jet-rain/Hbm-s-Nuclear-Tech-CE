package com.hbm.render.tileentity;

import com.hbm.interfaces.AutoRegister;
import com.hbm.render.NTMRenderHelper;
import com.hbm.tileentity.machine.TileEntityStructureMarker;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
@AutoRegister
public class RenderStructureMarker extends TileEntitySpecialRenderer<TileEntityStructureMarker> {

	final float pixel = 1F / 16F;
	public static final TextureAtlasSprite[][] fusion =
		{ 
		{ null, null }, 
		{ null, null }, 
		{ null, null }, 
		{ null, null }, 
		{ null, null }, 
		{ null, null }, 
		{ null, null }, 
		};
	public static final TextureAtlasSprite[][] watz = 
		{ 
		{ null, null }, 
		{ null, null }, 
		{ null, null }, 
		{ null, null }, 
		{ null, null }, 
		{ null, null }, 
		{ null, null }, 
		{ null, null }, 
		};

	@Override
	public boolean isGlobalRenderer(TileEntityStructureMarker te) {
		return true;
	}
	
	@Override
	public void render(TileEntityStructureMarker te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		GlStateManager.pushMatrix();
		GlStateManager.translate((float) x, (float) y, (float) z);
		GlStateManager.rotate(180, 0F, 0F, 1F);

		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GlStateManager.glBlendEquation(GL14.GL_FUNC_ADD);
		GlStateManager.color(0.5f, 0.25f, 1.0f, 1f);
		NTMRenderHelper.startDrawingTexturedQuads();
		this.renderBlocks((int) x, (int) y, (int) z, te.type, te.getBlockMetadata());
		NTMRenderHelper.draw();
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	public void renderBlocks(int x, int y, int z, int type, int meta) {
		bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		int offsetX = 0;
		int offsetZ = 0;
		if(type == 0) {
			//System.out.println(meta);
			if(meta == 1) {
				offsetZ = -1;
				offsetX = 3;
			}
			if(meta == 2) {
				offsetZ = -1;
				offsetX = -1;
			}
			if(meta == 3) {
				offsetX = 1;
				offsetZ = -3;
			}
			if(meta == 4) {
				offsetZ = 1;
				offsetX = 1;
			}
			

			GlStateManager.translate(offsetX, 0, offsetZ);
		}
	}
}
