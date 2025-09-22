package com.hbm.render.tileentity;
import com.hbm.util.EnumUtil;
import net.minecraft.item.ItemStack;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.tileentity.bomb.TileEntityCrashedBomb;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import com.hbm.blocks.bomb.BlockCrashedBomb.EnumDudType;
import org.lwjgl.opengl.GL11;

import java.util.Random;

@AutoRegister
public class RenderCrashedBomb extends TileEntitySpecialRenderer<TileEntityCrashedBomb>
        implements IItemRendererProvider {

    public static Random rand = new Random();

@Override
    public void render(
            TileEntityCrashedBomb tile,
            double x,
            double y,
            double z,
            float partialTicks,
            int destroyStage,
            float alpha) {

    GlStateManager.pushMatrix();
    GlStateManager.translate(x + 0.5D, y, z + 0.5D);
    GlStateManager.disableCull();
    GlStateManager.enableLighting();

    rand.setSeed(tile.getBlockMetadata());
    double yaw = rand.nextDouble() * 360;
    double pitch = rand.nextDouble() * 45 + 45;
    double roll = rand.nextDouble() * 360;
    double offset = rand.nextDouble() * 2 - 1;

    GlStateManager.rotate(yaw, 0, 1, 0);
    GlStateManager.rotate(pitch, 1, 0, 0);
    GlStateManager.rotate(roll, 0, 0, 1);
    GlStateManager.translate(0, 0, -offset);

    EnumDudType type = EnumUtil.grabEnumSafely(EnumDudType.class, tile.getBlockMetadata());
    GlStateManager.shadeModel(GL11.GL_SMOOTH);
    switch (type) {
        case BALEFIRE:
        {
            bindTexture(ResourceManager.dud_balefire_tex);
            ResourceManager.dud_balefire.renderAll();
            break;
        }
        case CONVENTIONAL: {
            bindTexture(ResourceManager.dud_conventional_tex);
            ResourceManager.dud_conventional.renderAll();
            break;
        }
        case NUKE: {
            GlStateManager.translate(0, 0, 1.25);
            bindTexture(ResourceManager.dud_nuke_tex);
            ResourceManager.dud_nuke.renderAll();
            break;
        }
        case SALTED: {
            GlStateManager.translate(0, 0, 0.5);
            bindTexture(ResourceManager.dud_salted_tex);
            ResourceManager.dud_salted.renderAll();
            break;
        }

    }
    GlStateManager.shadeModel(GL11.GL_FLAT);
    GlStateManager.enableCull();
    GlStateManager.popMatrix();
}
    @Override
    public Item getItemForRenderer() {
        return Item.getItemFromBlock(ModBlocks.crashed_bomb);
    }

    @Override
    public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {

            public void renderInventory() {
                GlStateManager.translate(0, 3, 0);
                GlStateManager.scale(2.125, 2.125, 2.125);
                GlStateManager.rotate(90, 0, 0, 1);
            }
            public void renderCommonWithStack(ItemStack item) {
                EnumDudType type = EnumUtil.grabEnumSafely(EnumDudType.class, item.getItemDamage());
                GlStateManager.rotate(90, 0, 1, 0);
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                switch (type) {
                    case BALEFIRE: {
                        bindTexture(ResourceManager.dud_balefire_tex);
                        ResourceManager.dud_balefire.renderAll();
                        break;
                    }
                    case CONVENTIONAL: {
                        GlStateManager.translate(0, 0, -0.5);
                        bindTexture(ResourceManager.dud_conventional_tex);
                        ResourceManager.dud_conventional.renderAll();
                        break;
                    }
                    case NUKE: {
                        GlStateManager.translate(0, 0, 1.25);
                        bindTexture(ResourceManager.dud_nuke_tex);
                        ResourceManager.dud_nuke.renderAll();
                        break;
                    }
                    case SALTED: {
                        GlStateManager.translate(0, 0, 0.5);
                        bindTexture(ResourceManager.dud_salted_tex);
                        ResourceManager.dud_salted.renderAll();
                        break;
                    }

                }
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        };
    }
}