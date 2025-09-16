package com.hbm.render.tileentity;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import org.lwjgl.opengl.GL11;
@AutoRegister
public class RenderChemicalFactory extends TileEntitySpecialRenderer<TileEntityMachineChemicalFactory> implements IItemRendererProvider {

    @Override
    public void render(TileEntityMachineChemicalFactory chemplant, double x, double y, double z, float interp, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        GlStateManager.rotate(90, 0, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        switch (chemplant.getBlockMetadata() - BlockDummyable.offset) {
            case 2 -> GlStateManager.rotate(0, 0F, 1F, 0F);
            case 4 -> GlStateManager.rotate(90, 0F, 1F, 0F);
            case 3 -> GlStateManager.rotate(180, 0F, 1F, 0F);
            case 5 -> GlStateManager.rotate(270, 0F, 1F, 0F);
        }
        float anim = chemplant.prevAnim + (chemplant.anim - chemplant.prevAnim) * interp;

        bindTexture(ResourceManager.chemical_factory_tex);
        ResourceManager.chemical_factory.renderPart("Base");
        if(chemplant.frame) ResourceManager.chemical_factory.renderPart("Frame");

        GlStateManager.pushMatrix();
        GlStateManager.translate(1, 0, 0);
        GlStateManager.rotate(-anim * 45 % 360D, 0, 1, 0);
        GlStateManager.translate(-1, 0, 0);
        ResourceManager.chemical_factory.renderPart("Fan1");
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.translate(-1, 0, 0);
        GlStateManager.rotate(-anim * 45 % 360D, 0, 1, 0);
        GlStateManager.translate(1, 0, 0);
        ResourceManager.chemical_factory.renderPart("Fan2");
        GlStateManager.popMatrix();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }

    @Override
    public Item getItemForRenderer() {
        return Item.getItemFromBlock(ModBlocks.machine_chemical_factory);
    }

    @Override
    public ItemRenderBase getRenderer(Item item) {

        return new ItemRenderBase() {

            public void renderInventory() {
                GlStateManager.translate(0, -1.5, 0);
                GlStateManager.scale(3, 3, 3);
            }
            public void renderCommon() {
                GlStateManager.scale(0.75, 0.75, 0.75);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                bindTexture(ResourceManager.chemical_factory_tex);
                ResourceManager.chemical_factory.renderPart("Base");
                ResourceManager.chemical_factory.renderPart("Frame");
                ResourceManager.chemical_factory.renderPart("Fan1");
                ResourceManager.chemical_factory.renderPart("Fan2");
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }};
    }
}
