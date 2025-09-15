package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import com.hbm.items.IDynamicModels;
import com.hbm.lib.RefStrings;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;

import static com.hbm.render.block.BlockBakeFrame.ROOT_PATH;

public class BlockBakedLayered extends BlockBase implements IDynamicModels {
    protected String texturePath;
    protected TextureAtlasSprite textureSprite;
    public final int MAX_LAYRES = 8;
    PropertyInteger LAYERS = PropertyInteger.create("layers", 1, MAX_LAYRES);

    public BlockBakedLayered(Material m, SoundType sound, String s, String texturePath) {
        super(m, sound, s);
        this.texturePath = texturePath;
    }

    public BlockBakedLayered(Material m, SoundType sound, String s, TextureAtlasSprite textureAtlasSprite) {
        super(m, sound, s);
        this.textureSprite = textureAtlasSprite;
        this.texturePath = texturePath;
    }

    @Override
    public StateMapperBase getStateMapper(ResourceLocation loc) {
        return IDynamicModels.super.getStateMapper(loc);
    }

    @Override
    public void bakeModel(ModelBakeEvent event) {

        for(int i = 0; i < MAX_LAYRES; i++){
            int layerIndex = 2;
//            ModelLoaderRegistry.getModelOrMissing();
        }


    }

    @Override
    public void registerModel() {


    }

    @Override
    public void registerSprite(TextureMap map) {
        if (textureSprite != null) {
            map.setTextureEntry(textureSprite);
        } else {
            map.registerSprite(new ResourceLocation(RefStrings.MODID, ROOT_PATH + texturePath));
        }
    }
}
