package com.snailmod.entity;

import com.snailmod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A slow, peaceful snail. When it takes damage it retracts into its shell for a few
 * seconds: it stops moving and takes only a quarter of incoming damage.
 * Breeds with mushrooms, spawns in swamps and forests.
 */
public class SnailEntity extends Animal {
    private static final EntityDataAccessor<Boolean> HIDING =
            SynchedEntityData.defineId(SnailEntity.class, EntityDataSerializers.BOOLEAN);

    /** How long the snail stays in its shell after being hit (5 seconds). */
    private static final int HIDE_DURATION = 100;
    /** Fraction of damage that gets through the shell. */
    private static final float SHELL_DAMAGE_FACTOR = 0.25F;

    private int hideTicks;

    // Client-side animation state: 0 = fully out, 1 = fully inside the shell.
    private float hideProgress;
    private float prevHideProgress;

    public SnailEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.12)
                .add(Attributes.ARMOR, 2.0);
    }

    public static boolean checkSnailSpawnRules(EntityType<SnailEntity> type, LevelAccessor level,
                                               MobSpawnType reason, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        boolean goodGround = below.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || below.is(Blocks.MOSS_BLOCK)
                || below.is(Blocks.MUD);
        return goodGround && isBrightEnoughToSpawn(level, pos);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.2, this::isFood, false));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 4.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HIDING, false);
    }

    public boolean isHiding() {
        return this.entityData.get(HIDING);
    }

    private void setHiding(boolean hiding) {
        this.entityData.set(HIDING, hiding);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        // The shell protects against everything except the void and /kill.
        if (this.isHiding() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= SHELL_DAMAGE_FACTOR;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.isAlive()) {
            this.hideTicks = HIDE_DURATION;
            this.setHiding(true);
            this.getNavigation().stop();
        }
        return hurt;
    }

    /** While hiding, vanilla skips all AI and movement input. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isHiding();
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide && this.hideTicks > 0 && --this.hideTicks == 0) {
            this.setHiding(false);
        }
        super.aiStep();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.prevHideProgress = this.hideProgress;
            float target = this.isHiding() ? 1.0F : 0.0F;
            // Retract quickly, peek out slowly.
            float step = this.isHiding() ? 0.35F : 0.08F;
            this.hideProgress = Mth.approach(this.hideProgress, target, step);
        }
    }

    public float getHideProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevHideProgress, this.hideProgress);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.BROWN_MUSHROOM) || stack.is(Items.RED_MUSHROOM);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.SNAIL.get().create(level);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HideTicks", this.hideTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.hideTicks = tag.getInt("HideTicks");
        this.setHiding(this.hideTicks > 0);
    }

    // Snails are quiet: no ambient or step sounds, just a little squish.
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_SQUISH_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }
}
