package com.hbm.render.tileentity;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.machine.TileEntityElectrolyser;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import org.lwjgl.opengl.GL11;
@AutoRegister
public class RenderElectrolyser extends TileEntitySpecialRenderer<TileEntityElectrolyser> implements IItemRendererProvider{

    @Override
    public void render(TileEntityElectrolyser te, double x, double y, double z, float interp, int destroyStage, float alpha) {

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y, z + 0.5D);

        switch(te.getBlockMetadata() - BlockDummyable.offset) {
            case 4: GlStateManager.rotate(90, 0F, 1F, 0F); break;
            case 3: GlStateManager.rotate(180, 0F, 1F, 0F); break;
            case 5: GlStateManager.rotate(270, 0F, 1F, 0F); break;
            case 2: GlStateManager.rotate(0, 0F, 1F, 0F); break;
        }

        GL11.glRotated(180, 0, 1, 0);
        GlStateManager.enableLighting();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        bindTexture(ResourceManager.electrolyser_tex);
        ResourceManager.electrolyser.renderAll();

        GlStateManager.shadeModel(GL11.GL_FLAT);

        GlStateManager.popMatrix();

    }

    public boolean isGlobalRenderer(TileEntityElectrolyser te)
    {
        return true;
    }


    @Override
    public Item getItemForRenderer() {
        return Item.getItemFromBlock(ModBlocks.machine_electrolyser);
    }
    @Override
    public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase( ) {
            public void renderInventory() {
                GlStateManager.translate(-1, -1, 0);
                GL11.glScaled(2.5, 2.5, 2.5);
            }
            public void renderCommon() {
                GL11.glScaled(0.5, 0.5, 0.5);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                bindTexture(ResourceManager.electrolyser_tex); ResourceManager.electrolyser.renderAll();
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        };
    }
}
