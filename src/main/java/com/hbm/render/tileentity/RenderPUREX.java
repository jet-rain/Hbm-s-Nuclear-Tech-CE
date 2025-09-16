package com.hbm.render.tileentity;

import com.hbm.interfaces.AutoRegister;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.machine.TileEntityMachinePUREX;
import com.hbm.util.BobMathUtil;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@AutoRegister
public class RenderPUREX extends TileEntitySpecialRenderer<TileEntityMachinePUREX> implements IItemRendererProvider {

    @Override
    public void render(TileEntityMachinePUREX purex, double x, double y, double z, float interp, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        switch(purex.getBlockMetadata() - BlockDummyable.offset) {
            case 2: GlStateManager.rotate(0, 0F, 1F, 0F); break;
            case 4: GlStateManager.rotate(90, 0F, 1F, 0F); break;
            case 3: GlStateManager.rotate(180, 0F, 1F, 0F); break;
            case 5: GlStateManager.rotate(270, 0F, 1F, 0F); break;
        }

        float anim = purex.prevAnim + (purex.anim - purex.prevAnim) * interp;

        bindTexture(ResourceManager.purex_tex);
        ResourceManager.purex.renderPart("Base");
        if(purex.frame)ResourceManager.purex.renderPart("Frame");

        GlStateManager.pushMatrix();
        GlStateManager.translate(1.5, 1.25, 0);
        GlStateManager.rotate(anim * 45, 0, 0, 1);
        GlStateManager.translate(-1.5, -1.25, 0);
        ResourceManager.purex.renderPart("Fan");
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.translate(BobMathUtil.sps(anim * 0.25) * 0.5, 0, 0);
        ResourceManager.purex.renderPart("Pump");
        GlStateManager.popMatrix();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }

    @Override
    public Item getItemForRenderer() {
        return Item.getItemFromBlock(ModBlocks.machine_purex);
    }

    // mlbv: the translations here hasn't been calibrated and might be incorrect
    @Override
    public ItemRenderBase getRenderer(Item item) {

        return new ItemRenderBase() {

            public void renderInventory() {
                GlStateManager.translate(0, -2.5, 0);
                GlStateManager.scale(2.5, 2.5, 2.5);
            }

            public void renderCommon(ItemStack item) {
                GlStateManager.scale(0.75, 0.75, 0.75);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                bindTexture(ResourceManager.purex_tex);
                ResourceManager.purex.renderPart("Base");
                ResourceManager.purex.renderPart("Frame");
                ResourceManager.purex.renderPart("Fan");
                ResourceManager.purex.renderPart("Pump");
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }};
    }
}
