package com.cuteferrets.entity.goal;

import com.cuteferrets.CuteFerrets;
import com.cuteferrets.entity.FerretEntity;
import com.cuteferrets.entity.FerretMode;
import com.cuteferrets.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.EnumSet;
import java.util.List;

/**
 * In treasure mode the ferret stops every now and then on dirt, sand or gravel, digs for a couple of
 * seconds and pulls out something from the "ferret_digging" loot table into its pouch.
 */
public class FerretTreasureDigGoal extends Goal {
    private static final ResourceKey<LootTable> OVERWORLD_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, CuteFerrets.id("gameplay/ferret_digging"));
    private static final ResourceKey<LootTable> NETHER_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, CuteFerrets.id("gameplay/ferret_digging_nether"));
    private static final int DIG_TIME = 60;

    private final FerretEntity ferret;
    private int cooldown;
    private int digTicks;

    public FerretTreasureDigGoal(FerretEntity ferret) {
        this.ferret = ferret;
        this.cooldown = 100;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (ferret.getMode() != FerretMode.DIG || !ferret.canWork()) {
            return false;
        }
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!ferret.onGround() || ferret.isInWater() || !ferret.hasFreeSpace()) {
            return false;
        }
        // don't wander off digging while the owner is walking away
        if (ferret.getOwner() != null && ferret.distanceToSqr(ferret.getOwner()) > 10 * 10) {
            return false;
        }
        return ferret.level().getBlockState(groundPos()).is(ModTags.FERRET_DIGGABLE);
    }

    @Override
    public boolean canContinueToUse() {
        return digTicks > 0 && ferret.getMode() == FerretMode.DIG && ferret.canWork()
                && ferret.level().getBlockState(groundPos()).is(ModTags.FERRET_DIGGABLE);
    }

    @Override
    public void start() {
        digTicks = DIG_TIME;
        ferret.getNavigation().stop();
        ferret.setDigging(true);
    }

    @Override
    public void stop() {
        ferret.setDigging(false);
        cooldown = ferret.nextDigCooldown();
        digTicks = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Level level = ferret.level();
        BlockPos ground = groundPos();
        BlockState state = level.getBlockState(ground);
        ferret.getLookControl().setLookAt(ferret.getX(), ferret.getY() - 1, ferret.getZ() + 0.01);

        if (digTicks % 4 == 0 && level instanceof ServerLevel server) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    ferret.getX(), ferret.getY() + 0.1, ferret.getZ(), 6, 0.2, 0.05, 0.2, 0.1);
            level.playSound(null, ferret.blockPosition(), state.getSoundType().getHitSound(),
                    SoundSource.NEUTRAL, 0.5F, 1.2F);
        }

        if (--digTicks <= 0 && level instanceof ServerLevel server) {
            findTreasure(server);
        }
    }

    private void findTreasure(ServerLevel level) {
        ResourceKey<LootTable> key = level.dimension() == Level.NETHER ? NETHER_LOOT : OVERWORLD_LOOT;
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, ferret.position())
                .withParameter(LootContextParams.THIS_ENTITY, ferret)
                .withLuck(ferret.getDigLuck())
                .create(LootContextParamSets.GIFT);
        List<ItemStack> loot = table.getRandomItems(params);
        if (loot.isEmpty()) {
            // nothing this time, but the ferret is still having fun
            ferret.playSound(net.minecraft.sounds.SoundEvents.FOX_SNIFF, 0.6F, 1.6F);
            return;
        }
        for (ItemStack stack : loot) {
            ferret.celebrateFind(stack.copy());
            ferret.storeOrDrop(stack);
        }
    }

    private BlockPos groundPos() {
        return BlockPos.containing(ferret.getX(), ferret.getY() - 0.2, ferret.getZ());
    }
}
