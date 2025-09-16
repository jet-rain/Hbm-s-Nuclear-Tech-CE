package com.hbm.render.entity;

import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class RenderMetaSensitiveItem<T extends Entity & RenderMetaSensitiveItem.IHasMetaSensitiveRenderer<T>> extends RenderSnowball<T> {
    public RenderMetaSensitiveItem(RenderManager renderManagerIn, Item itemIn, RenderItem itemRendererIn) {
        super(renderManagerIn, itemIn, itemRendererIn);
    }

    @Override
    public ItemStack getStackToRender(T entityIn) {
        return entityIn.getStackToRender(entityIn);
    }

    public interface IHasMetaSensitiveRenderer<T extends Entity> {
        ItemStack getStackToRender(T entityIn);
    }
}
