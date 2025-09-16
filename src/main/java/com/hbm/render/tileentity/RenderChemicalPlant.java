package com.hbm.render.tileentity;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import com.hbm.util.BobMathUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@AutoRegister
public class RenderChemicalPlant extends TileEntitySpecialRenderer<TileEntityMachineChemicalPlant> implements IItemRendererProvider {

    @Override
    public void render(TileEntityMachineChemicalPlant chemplant, double x, double y, double z, float interp, int destroyStage, float alpha) {
        GlStateManager.enableAlpha();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        GlStateManager.rotate(90F, 0F, 1F, 0F);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        switch (chemplant.getBlockMetadata() - BlockDummyable.offset) {
            case 2 -> GlStateManager.rotate(0F, 0F, 1F, 0F);
            case 4 -> GlStateManager.rotate(90F, 0F, 1F, 0F);
            case 3 -> GlStateManager.rotate(180F, 0F, 1F, 0F);
            case 5 -> GlStateManager.rotate(270F, 0F, 1F, 0F);
        }
        float anim = chemplant.prevAnim + (chemplant.anim - chemplant.prevAnim) * interp;
        GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.recipeNameMap.get(chemplant.chemplantModule.recipe);

        bindTexture(ResourceManager.chemical_plant_tex);
        ResourceManager.chemical_plant.renderPart("Base");
        if (chemplant.frame) ResourceManager.chemical_plant.renderPart("Frame");

        GlStateManager.pushMatrix();
        GlStateManager.translate(BobMathUtil.sps(anim * 0.125) * 0.375, 0, 0);
        ResourceManager.chemical_plant.renderPart("Slider");
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5, 0, 0.5);
        GlStateManager.rotate((anim * 15) % 360F, 0, 1, 0);
        GlStateManager.translate(-0.5, 0, -0.5);
        ResourceManager.chemical_plant.renderPart("Spinner");
        GlStateManager.popMatrix();

        if (chemplant.didProcess && recipe != null) {
            int colors = 0;
            int r = 0, g = 0, b = 0;

            if (recipe.outputFluid != null) {
                for (FluidStack stack : recipe.outputFluid) {
                    Color c = new Color(stack.type.getColor());
                    r += c.getRed();
                    g += c.getGreen();
                    b += c.getBlue();
                    colors++;
                }
            }

            if (colors == 0 && recipe.inputFluid != null) {
                for (FluidStack stack : recipe.inputFluid) {
                    Color c = new Color(stack.type.getColor());
                    r += c.getRed();
                    g += c.getGreen();
                    b += c.getBlue();
                    colors++;
                }
            }

            if (colors > 0) {
                bindTexture(ResourceManager.chemical_plant_fluid_tex);

                GlStateManager.pushMatrix();
                GlStateManager.disableLighting();
                GlStateManager.enableBlend();
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                GlStateManager.color((r / 255F) / colors, (g / 255F) / colors, (b / 255F) / colors, 0.5F);
                GlStateManager.depthMask(false);

                GlStateManager.matrixMode(GL11.GL_TEXTURE);
                GlStateManager.loadIdentity();
                GlStateManager.translate(-anim / 100F, BobMathUtil.sps(anim * 0.1) * 0.1 - 0.25, 0);
                ResourceManager.chemical_plant.renderPart("Fluid");
                GlStateManager.matrixMode(GL11.GL_TEXTURE);
                GlStateManager.loadIdentity();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);

                GlStateManager.depthMask(true);
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
                GlStateManager.disableBlend();
                GlStateManager.enableLighting();
                GlStateManager.color(1F, 1F, 1F, 1F);

                GlStateManager.popMatrix();
            }
        }

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }

    @Override
    public Item getItemForRenderer() {
        return Item.getItemFromBlock(ModBlocks.machine_chemical_plant);
    }

    @Override
    public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            @Override
            public void renderInventory() {
                GlStateManager.translate(0, -2.75, 0);
                GlStateManager.scale(4.5, 4.5, 4.5);
            }

            @Override
            public void renderCommon() {
                GlStateManager.rotate(90F, 0F, 1F, 0F);
                GlStateManager.scale(0.75, 0.75, 0.75);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                bindTexture(ResourceManager.chemical_plant_tex);
                ResourceManager.chemical_plant.renderPart("Base");
                ResourceManager.chemical_plant.renderPart("Slider");
                ResourceManager.chemical_plant.renderPart("Spinner");
                ResourceManager.chemical_plant.renderPart("Frame");
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        };
    }
}
