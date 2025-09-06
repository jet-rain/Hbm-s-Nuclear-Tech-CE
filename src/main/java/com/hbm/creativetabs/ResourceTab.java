package com.hbm.creativetabs;

import com.hbm.blocks.ModBlocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ResourceTab extends CreativeTabs {

	public ResourceTab(int index, String label) {
		super(index, label);
	}

	@Override
    @SideOnly(Side.CLIENT)
	public ItemStack createIcon() {
		if(ModBlocks.ore_uranium != null){
			return new ItemStack(Item.getItemFromBlock(ModBlocks.ore_uranium));
		}
		return new ItemStack(Items.IRON_PICKAXE);
	}

}
