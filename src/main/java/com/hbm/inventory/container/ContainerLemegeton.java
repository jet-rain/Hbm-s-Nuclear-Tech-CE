package com.hbm.inventory.container;

import com.hbm.inventory.recipes.LemegetonRecipes;
import com.hbm.items.ModItems;

import com.hbm.lib.Library;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;

public class ContainerLemegeton extends Container {

	public InventoryCrafting getStackInSlot = new InventoryCrafting(this, 1, 1);
	public IInventory craftResult = new InventoryCraftResult();

	public ContainerLemegeton(InventoryPlayer inventory) {

		this.addSlotToContainer(new SlotCrafting(inventory.player, this.getStackInSlot, this.craftResult, 0, 107, 35));
		this.addSlotToContainer(new Slot(this.getStackInSlot, 0, 49, 35));

		for(int l = 0; l < 3; ++l) {
			for(int i1 = 0; i1 < 9; ++i1) {
				this.addSlotToContainer(new Slot(inventory, i1 + l * 9 + 9, 8 + i1 * 18, 84 + l * 18));
			}
		}

		for(int l = 0; l < 9; ++l) {
			this.addSlotToContainer(new Slot(inventory, l, 8 + l * 18, 142));
		}

		this.onCraftMatrixChanged(this.getStackInSlot);
	}

	public void onCraftMatrixChanged(IInventory inventoryIn) {
		this.craftResult.setInventorySlotContents(0, LemegetonRecipes.getRecipe(getStackInSlot.getStackInSlot(0)));
	}

	public void onContainerClosed(EntityPlayer player) {
		super.onContainerClosed(player);

		if(!player.world.isRemote) {
			this.clearContainer(player, player.world, this.getStackInSlot);
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotNo) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = (Slot) this.inventorySlots.get(slotNo);

		if(slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();

			if(slotNo <= 1) {
				if(!this.mergeItemStack(itemstack1, 2, this.inventorySlots.size(), true)) {
					return ItemStack.EMPTY;
				}

				slot.onSlotChange(itemstack1, itemstack);
			} else if(!this.mergeItemStack(itemstack1, 1, 2, false)) {
				return ItemStack.EMPTY;
			}

			if(itemstack1.getCount() == 0) {
				slot.putStack(ItemStack.EMPTY);
			} else {
				slot.onSlotChanged();
			}

			if(itemstack1.getCount() == itemstack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTake(player, itemstack1);
		}

		return itemstack;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return Library.hasInventoryItem(player.inventory, ModItems.book_lemegeton);
	}

	@Override
	public boolean canMergeSlot(ItemStack stack, Slot slot) {
		return slot.inventory != this.craftResult && super.canMergeSlot(stack, slot);
	}
}
