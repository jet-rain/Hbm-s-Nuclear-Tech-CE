package com.hbm.render.block;

import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;

public class LayeredStateMapper extends StateMapperBase {

    private final String blockName;
    private final PropertyInteger layers;

    public LayeredStateMapper(String blockName, PropertyInteger layers) {
        this.blockName = blockName;
        this.layers = layers;
    }

    @Override
    protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
        int layer = state.getValue(layers);
        return new ModelResourceLocation(blockName + "_layer" + layer, "normal");
    }
}
