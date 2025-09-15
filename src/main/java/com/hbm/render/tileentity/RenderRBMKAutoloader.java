package com.hbm.render.tileentity;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKAutoloader;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import org.lwjgl.opengl.GL11;

@AutoRegister
public class RenderRBMKAutoloader extends TileEntitySpecialRenderer<TileEntityRBMKAutoloader> implements IItemRendererProvider {

    @Override
    public void render(TileEntityRBMKAutoloader press, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y, z + 0.5D);
        GlStateManager.enableCull();

        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        bindTexture(ResourceManager.rbmk_autoloader_tex);
        ResourceManager.rbmk_autoloader.renderPart("Base");

        double p = (press.lastPiston + (press.renderPiston - press.lastPiston) * partialTicks);
        GlStateManager.translate(0, p * -4D, 0);

        GlStateManager.translate(0, 4, 0);
        ResourceManager.rbmk_autoloader.renderPart("Piston");
        GlStateManager.shadeModel(GL11.GL_FLAT);

        GlStateManager.popMatrix();
    }

    @Override
    public Item getItemForRenderer() {
        return Item.getItemFromBlock(ModBlocks.rbmk_autoloader);
    }

    @Override
    public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            public void renderInventory() {
                GlStateManager.translate(0, -6, 0);
                GlStateManager.scale(1.75, 1.75, 1.75);
            }

            public void renderCommon() {
                GlStateManager.rotate(180, 0, 1, 0);
                GlStateManager.disableCull();
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                bindTexture(ResourceManager.rbmk_autoloader_tex);
                ResourceManager.rbmk_autoloader.renderPart("Base");
                ResourceManager.rbmk_autoloader.renderPart("Piston");
                GlStateManager.shadeModel(GL11.GL_FLAT);
                GlStateManager.enableCull();
            }
        };
    }
}
