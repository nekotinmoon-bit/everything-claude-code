package com.cuteferrets.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A tasty treat: tames a wild ferret instantly; for your own ferret it's a big boost of love and luck. */
public class FerretTreatItem extends Item {
    public FerretTreatItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.cuteferrets.ferret_treat.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
