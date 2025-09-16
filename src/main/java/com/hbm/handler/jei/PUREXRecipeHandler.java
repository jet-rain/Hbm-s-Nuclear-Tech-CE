package com.hbm.handler.jei;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.PUREXRecipes;
import mezz.jei.api.IGuiHelper;

public class PUREXRecipeHandler extends JEIGenericRecipeHandler {

    public PUREXRecipeHandler(IGuiHelper helper) {
        super(helper, JEIConfig.PUREX, ModBlocks.machine_purex.getTranslationKey(), PUREXRecipes.INSTANCE, ModBlocks.machine_purex);
    }

}
