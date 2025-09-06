package com.hbm.creativetabs;

import com.hbm.items.ModItems;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class WeaponTab extends CreativeTabs {

	public WeaponTab(int index, String label) {
		super(index, label);
	}

	@Override
    @SideOnly(Side.CLIENT)
	public ItemStack createIcon() {
		if(ModItems.gun_vortex != null){
			return new ItemStack(ModItems.gun_vortex);
		}
		return new ItemStack(Items.IRON_PICKAXE);
	}

}
