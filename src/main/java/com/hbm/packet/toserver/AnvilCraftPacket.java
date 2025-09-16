package com.hbm.packet.toserver;

import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.inventory.container.ContainerAnvil;
import com.hbm.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class AnvilCraftPacket implements IMessage {

	int recipeIndex;
	int mode;

	public AnvilCraftPacket() { }

	public AnvilCraftPacket(AnvilConstructionRecipe recipe, int mode) {
		this.recipeIndex = AnvilRecipes.getConstruction().indexOf(recipe);
		this.mode = mode;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.recipeIndex = buf.readInt();
		this.mode = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.recipeIndex);
		buf.writeInt(this.mode);
	}

	public static class Handler implements IMessageHandler<AnvilCraftPacket, IMessage> {
		
		@Override
		public IMessage onMessage(AnvilCraftPacket m, MessageContext ctx) {

			ctx.getServerHandler().player.server.addScheduledTask(() -> {
				if(m.recipeIndex < 0 || m.recipeIndex >= AnvilRecipes.getConstruction().size()) //recipe is out of range -> bad
					return;

				EntityPlayer p = ctx.getServerHandler().player;
				
				if(!(p.openContainer instanceof ContainerAnvil)) //player isn't even using an anvil -> bad
					return;
				
				ContainerAnvil anvil = (ContainerAnvil)p.openContainer;
				AnvilConstructionRecipe recipe = AnvilRecipes.getConstruction().get(m.recipeIndex);

				if(!recipe.isTierValid(anvil.tier)) //player is using the wrong type of anvil -> bad
					return;

				int count = m.mode == 1 ? 64 : 1;
				
				for(int i = 0; i < count; i++) {
					
					if(InventoryUtil.doesPlayerHaveAStacks(p, recipe.input, true)) {
						InventoryUtil.giveChanceStacksToPlayer(p, recipe.output);
					} else {
						break;
					}
				}
				
				p.inventoryContainer.detectAndSendChanges();
			});
			
			return null;
		}
	}
}