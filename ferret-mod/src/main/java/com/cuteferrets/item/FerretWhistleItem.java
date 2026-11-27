package com.cuteferrets.item;

import com.cuteferrets.entity.FerretEntity;
import com.cuteferrets.entity.FerretMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Right-click: every owned ferret nearby comes running (far ones are teleported) and switches to "follow".
 * Sneak + right-click: every owned ferret nearby lies down and waits.
 */
public class FerretWhistleItem extends Item {
    private static final double RANGE = 64.0;

    public FerretWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean sit = player.isShiftKeyDown();
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS,
                1.0F, sit ? 0.9F : 1.6F);
        if (!level.isClientSide) {
            AABB area = player.getBoundingBox().inflate(RANGE);
            List<FerretEntity> ferrets = level.getEntitiesOfClass(FerretEntity.class, area, f -> f.isOwnedBy(player));
            for (FerretEntity ferret : ferrets) {
                if (sit) {
                    ferret.setMode(FerretMode.SIT);
                } else {
                    ferret.setMode(FerretMode.FOLLOW);
                    if (ferret.distanceToSqr(player) > 12 * 12) {
                        ferret.teleportTo(player.getX(), player.getY(), player.getZ());
                    }
                }
            }
            player.displayClientMessage(Component.translatable(
                    sit ? "message.cuteferrets.whistle_sit" : "message.cuteferrets.whistle_follow", ferrets.size()), true);
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.cuteferrets.ferret_whistle.tooltip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.cuteferrets.ferret_whistle.tooltip2").withStyle(ChatFormatting.GRAY));
    }
}
