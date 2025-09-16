package com.hbm.modules.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes;

import net.minecraftforge.items.ItemStackHandler;

public class ModuleMachinePUREX extends ModuleMachineBase {

    public ModuleMachinePUREX(int index, IEnergyHandlerMK2 battery, ItemStackHandler slots) {
        super(index, battery, slots);
        this.inputSlots = new int[3];
        this.outputSlots = new int[6];
        this.inputTanks = new FluidTankNTM[3];
        this.outputTanks = new FluidTankNTM[1];
    }

    @Override
    public GenericRecipes getRecipeSet() {
        return PUREXRecipes.INSTANCE;
    }

    public ModuleMachinePUREX itemInput(int start) { for(int i = 0; i < inputSlots.length; i++) inputSlots[i] = start + i; return this; }
    public ModuleMachinePUREX itemOutput(int start) { for(int i = 0; i < outputSlots.length; i++) outputSlots[i] = start + i; return this; }
    public ModuleMachinePUREX fluidInput(FluidTankNTM a, FluidTankNTM b, FluidTankNTM c) { inputTanks[0] = a; inputTanks[1] = b; inputTanks[2] = c; return this; }
    public ModuleMachinePUREX fluidOutput(FluidTankNTM a) { outputTanks[0] = a; return this; }
}
