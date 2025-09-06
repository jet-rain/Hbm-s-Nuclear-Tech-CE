package com.hbm.creativetabs;

import com.hbm.items.ModItems;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PartsTab extends CreativeTabs {

	public PartsTab(int index, String label) {
		super(index, label);
	}

	@Override
    @SideOnly(Side.CLIENT)
	public ItemStack createIcon() {
		if(ModItems.ingot_uranium != null){
			return new ItemStack(ModItems.ingot_uranium);
		}
		return new ItemStack(Items.IRON_PICKAXE);
	}

}
