package com.hbm.handler.jei;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import mezz.jei.api.IGuiHelper;
import net.minecraft.item.ItemStack;

public class AssemblyMachineRecipeHandler extends JEIGenericRecipeHandler {

    public AssemblyMachineRecipeHandler(IGuiHelper helper) {
        super(helper, JEIConfig.ASSEMBLY_MACHINE, ModBlocks.machine_assembly_machine.getTranslationKey(), AssemblyMachineRecipes.INSTANCE, new ItemStack(ModBlocks.machine_assembly_machine));
    }

    @Override public int getInputXOffset(GenericRecipe recipe, int inputCount) { return recipe.inputItem != null && recipe.inputItem.length > 9 ? 18 : 0; }
    @Override public int getOutputXOffset(GenericRecipe recipe, int outputCount) { return recipe.inputItem != null && recipe.inputItem.length > 9 ? 18 : 0; }
    @Override public int getMachineXOffset(GenericRecipe recipe) { return recipe.inputItem != null && recipe.inputItem.length > 9 ? 18 : 0; }
}
