package com.hbm.blocks;

import com.hbm.util.I18nUtil;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.util.List;

public interface ITooltipProvider {

    public void addInformation(
            ItemStack stack, @Nullable
            World worldIn, List<String> tooltip, ITooltipFlag flagIn);

    public default void addStandardInfo(List<String> list) {

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            for (String s : I18nUtil.resolveKeyArray(((Block) this).getTranslationKey() + ".desc"))
                list.add(TextFormatting.YELLOW + s);
        } else {
            list.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "Hold <" +
                    TextFormatting.YELLOW + "" + TextFormatting.ITALIC + "LSHIFT" +
                    TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "> to display more info");
        }
    }


    default EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.COMMON;
    }
}
