package com.cuteferrets.entity;

import com.cuteferrets.entity.goal.FerretFollowOwnerGoal;
import com.cuteferrets.entity.goal.FerretOreHuntGoal;
import com.cuteferrets.entity.goal.FerretTreasureDigGoal;
import com.cuteferrets.menu.FerretMenu;
import com.cuteferrets.registry.ModEntities;
import com.cuteferrets.registry.ModItems;
import com.cuteferrets.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;

public class FerretEntity extends TamableAnimal {
    public static final int INVENTORY_SIZE = 9;
    public static final int MAX_AFFECTION = 100;

    /** Entity event: owner petted the ferret (hearts on the client). */
    private static final byte EVENT_PETTED = 60;
    /** Entity event: ferret found something (happy sparkles on the client). */
    private static final byte EVENT_FOUND = 61;

    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(FerretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(FerretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AFFECTION =
            SynchedEntityData.defineId(FerretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DIGGING =
            SynchedEntityData.defineId(FerretEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_DANCE_TICKS =
            SynchedEntityData.defineId(FerretEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TREAT_TICKS =
            SynchedEntityData.defineId(FerretEntity.class, EntityDataSerializers.INT);

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);
    private int petCooldown;
    @Nullable
    private BlockPos homePos;

    public FerretEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setTame(false, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    // ------------------------------------------------------------------ AI

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.35F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(4, new FerretOreHuntGoal(this));
        this.goalSelector.addGoal(4, new FerretTreasureDigGoal(this));
        this.goalSelector.addGoal(5, new FerretFollowOwnerGoal(this, 1.2, 8.0F, 3.0F));
        this.goalSelector.addGoal(6, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(7, new TemptGoal(this, 1.1, stack -> stack.is(ModTags.FERRET_FOOD) || stack.is(ModItems.FERRET_TREAT.get()), false));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(4, new NonTameRandomTargetGoal<>(this, Rabbit.class, false, null));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Silverfish.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Endermite.class, true));
    }

    // ------------------------------------------------------------------ synced data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, 0);
        builder.define(DATA_MODE, FerretMode.FOLLOW.ordinal());
        builder.define(DATA_AFFECTION, 0);
        builder.define(DATA_DIGGING, false);
        builder.define(DATA_DANCE_TICKS, 0);
        builder.define(DATA_TREAT_TICKS, 0);
    }

    public FerretVariant getVariant() {
        return FerretVariant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(FerretVariant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
    }

    public FerretMode getMode() {
        return FerretMode.byId(this.entityData.get(DATA_MODE));
    }

    public void setMode(FerretMode mode) {
        this.entityData.set(DATA_MODE, mode.ordinal());
        this.setOrderedToSit(mode == FerretMode.SIT);
        this.setInSittingPose(mode == FerretMode.SIT);
        this.getNavigation().stop();
        this.setTarget(null);
        this.setDigging(false);
        if (mode == FerretMode.WANDER) {
            this.homePos = this.blockPosition();
            this.restrictTo(this.homePos, 12);
        } else {
            this.homePos = null;
            this.clearRestriction();
        }
    }

    public int getAffection() {
        return this.entityData.get(DATA_AFFECTION);
    }

    public void addAffection(int amount) {
        this.entityData.set(DATA_AFFECTION, Math.max(0, Math.min(MAX_AFFECTION, getAffection() + amount)));
    }

    public boolean isDigging() {
        return this.entityData.get(DATA_DIGGING);
    }

    public void setDigging(boolean digging) {
        this.entityData.set(DATA_DIGGING, digging);
    }

    public boolean isDancing() {
        return this.entityData.get(DATA_DANCE_TICKS) > 0;
    }

    public boolean hasTreatBuff() {
        return this.entityData.get(DATA_TREAT_TICKS) > 0;
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    /** Loot luck used for digging: grows with affection and with a treat. */
    public float getDigLuck() {
        return getAffection() / 25.0F + (hasTreatBuff() ? 2.0F : 0.0F);
    }

    /** Radius (in blocks) in which the ferret can smell ores. */
    public int getSniffRadius() {
        return 8 + getAffection() / 20 + (hasTreatBuff() ? 2 : 0);
    }

    /** Ticks to wait between two treasure digs. */
    public int nextDigCooldown() {
        int base = 500 - getAffection() * 3;
        if (hasTreatBuff()) {
            base /= 2;
        }
        return Math.max(100, base) + this.random.nextInt(120);
    }

    public boolean canWork() {
        return this.isTame() && !this.isOrderedToSit() && !this.isBaby() && !this.isDancing() && this.getTarget() == null;
    }

    /** Puts an item into the ferret's pouch; whatever does not fit is dropped on the ground. */
    public void storeOrDrop(ItemStack stack) {
        ItemStack rest = inventory.addItem(stack);
        if (!rest.isEmpty()) {
            this.spawnAtLocation(rest, 0.3F);
        }
    }

    public boolean hasFreeSpace() {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /** Gives every item from the pouch to the player; overflow drops at their feet. */
    public void giveLootTo(Player player) {
        int moved = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                moved += stack.getCount();
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
        }
        inventory.setChanged();
        if (moved > 0) {
            this.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.4F);
            this.level().broadcastEntityEvent(this, EVENT_FOUND);
        }
    }

    /** Called by the work goals when something was found. */
    public void celebrateFind(ItemStack found) {
        this.level().broadcastEntityEvent(this, EVENT_FOUND);
        this.playSound(SoundEvents.FOX_AMBIENT, 0.8F, 1.8F);
        if (this.getOwner() instanceof ServerPlayer owner && owner.distanceToSqr(this) < 48 * 48) {
            owner.displayClientMessage(Component.translatable("message.cuteferrets.found",
                    this.getDisplayName(), found.getCount(), found.getHoverName()), true);
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (petCooldown > 0) {
            petCooldown--;
        }
        int treat = this.entityData.get(DATA_TREAT_TICKS);
        if (treat > 0) {
            this.entityData.set(DATA_TREAT_TICKS, treat - 1);
        }
        int dance = this.entityData.get(DATA_DANCE_TICKS);
        if (dance > 0) {
            this.entityData.set(DATA_DANCE_TICKS, dance - 1);
            // the "weasel war dance": little hops and spins
            if (this.onGround() && dance % 8 == 0) {
                this.setDeltaMovement(this.getDeltaMovement().add(
                        (this.random.nextFloat() - 0.5F) * 0.25F, 0.32F, (this.random.nextFloat() - 0.5F) * 0.25F));
                this.setYRot(this.getYRot() + 60.0F + this.random.nextFloat() * 60.0F);
                this.yBodyRot = this.getYRot();
                this.playSound(SoundEvents.FOX_AMBIENT, 0.5F, 1.9F + this.random.nextFloat() * 0.2F);
            }
        }
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Level level = this.level();

        // ---- treat: big affection boost, instant taming, digging luck
        if (stack.is(ModItems.FERRET_TREAT.get())) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (!this.isTame()) {
                stack.consume(1, player);
                tameBy(player, true);
                return InteractionResult.SUCCESS;
            }
            if (this.isOwnedBy(player)) {
                stack.consume(1, player);
                this.heal(6.0F);
                this.addAffection(10);
                this.entityData.set(DATA_TREAT_TICKS, 20 * 120);
                this.startDance();
                level.broadcastEntityEvent(this, EVENT_PETTED);
                player.displayClientMessage(Component.translatable("message.cuteferrets.treat", this.getDisplayName()), true);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        // ---- wild ferret: tame with food
        if (!this.isTame()) {
            if (this.isFood(stack) && !this.isAngry()) {
                if (!level.isClientSide) {
                    stack.consume(1, player);
                    tameBy(player, false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return super.mobInteract(player, hand);
        }

        if (!this.isOwnedBy(player)) {
            return InteractionResult.PASS;
        }

        // ---- owner feeding: heal first, then breeding (vanilla)
        if (this.isFood(stack)) {
            if (this.getHealth() < this.getMaxHealth()) {
                if (!level.isClientSide) {
                    FoodProperties food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
                    this.heal(food != null ? food.nutrition() : 2.0F);
                    this.addAffection(2);
                    stack.consume(1, player);
                    this.playSound(SoundEvents.FOX_EAT, 1.0F, 1.5F);
                    level.broadcastEntityEvent(this, EVENT_PETTED);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            InteractionResult breed = super.mobInteract(player, hand);
            if (breed.consumesAction()) {
                if (!level.isClientSide) {
                    this.addAffection(2);
                }
                return breed;
            }
        }

        // ---- empty hand: pet / open menu
        if (stack.isEmpty()) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (player.isSecondaryUseActive()) {
                openMenu((ServerPlayer) player);
            } else {
                pet(player);
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    private boolean isAngry() {
        return this.getTarget() instanceof Player;
    }

    private void tameBy(Player player, boolean guaranteed) {
        if ((guaranteed || this.random.nextInt(3) == 0) && !ForgeEventFactory.onAnimalTame(this, player)) {
            this.tame(player);
            this.navigation.stop();
            this.setTarget(null);
            this.setMode(FerretMode.FOLLOW);
            this.addAffection(10);
            this.level().broadcastEntityEvent(this, (byte) 7);
            this.startDance();
            player.displayClientMessage(Component.translatable("message.cuteferrets.tamed", this.getDisplayName()), true);
        } else {
            this.level().broadcastEntityEvent(this, (byte) 6);
        }
    }

    private void pet(Player player) {
        this.level().broadcastEntityEvent(this, EVENT_PETTED);
        this.playSound(SoundEvents.FOX_SNIFF, 1.0F, 1.7F);
        if (petCooldown <= 0) {
            petCooldown = 20 * 30;
            this.addAffection(3);
            this.heal(1.0F);
            player.displayClientMessage(Component.translatable(
                    "message.cuteferrets.petted." + this.random.nextInt(4), this.getDisplayName()), true);
        }
        if (this.random.nextInt(3) == 0) {
            this.startDance();
        }
    }

    public void startDance() {
        this.entityData.set(DATA_DANCE_TICKS, 50);
        this.getNavigation().stop();
    }

    public void openMenu(ServerPlayer player) {
        this.getNavigation().stop();
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return FerretEntity.this.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player p) {
                return new FerretMenu(id, playerInventory, FerretEntity.this);
            }
        }, buf -> buf.writeVarInt(this.getId()));
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_PETTED) {
            spawnClientParticles(ParticleTypes.HEART, 5);
        } else if (id == EVENT_FOUND) {
            spawnClientParticles(ParticleTypes.HAPPY_VILLAGER, 8);
        } else {
            super.handleEntityEvent(id);
        }
    }

    private void spawnClientParticles(net.minecraft.core.particles.ParticleOptions particle, int count) {
        for (int i = 0; i < count; i++) {
            this.level().addParticle(particle, this.getRandomX(0.8), this.getRandomY() + 0.3, this.getRandomZ(0.8),
                    this.random.nextGaussian() * 0.02, this.random.nextGaussian() * 0.02, this.random.nextGaussian() * 0.02);
        }
    }

    // ------------------------------------------------------------------ breeding / food

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModTags.FERRET_FOOD);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        FerretEntity baby = ModEntities.FERRET.get().create(level);
        if (baby != null) {
            FerretVariant variant = this.getVariant();
            if (partner instanceof FerretEntity other && this.random.nextBoolean()) {
                variant = other.getVariant();
            }
            if (this.random.nextInt(10) == 0) {
                variant = FerretVariant.byId(this.random.nextInt(FerretVariant.values().length));
            }
            baby.setVariant(variant);
            if (this.getOwnerUUID() != null) {
                baby.setOwnerUUID(this.getOwnerUUID());
                baby.setTame(true, true);
                baby.setMode(FerretMode.FOLLOW);
            }
        }
        return baby;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        // albino and panda are rarer
        int roll = this.random.nextInt(100);
        FerretVariant variant;
        if (roll < 40) variant = FerretVariant.SABLE;
        else if (roll < 65) variant = FerretVariant.CHAMPAGNE;
        else if (roll < 85) variant = FerretVariant.BLACK;
        else if (roll < 95) variant = FerretVariant.ALBINO;
        else variant = FerretVariant.PANDA;
        this.setVariant(variant);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // ------------------------------------------------------------------ combat / death

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof TamableAnimal tamable && tamable.isTame() && tamable.getOwner() == owner) {
            return false;
        }
        if (target instanceof Player targetPlayer && owner instanceof Player ownerPlayer && !ownerPlayer.canHarmPlayer(targetPlayer)) {
            return false;
        }
        return super.wantsToAttack(target, owner);
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                ItemEntity item = this.spawnAtLocation(stack);
                if (item != null) {
                    item.setExtendedLifetime();
                }
            }
        }
    }

    // ------------------------------------------------------------------ sounds

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isInSittingPose()) {
            return this.random.nextInt(4) == 0 ? SoundEvents.FOX_SLEEP : null;
        }
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.6F + (this.random.nextFloat() - 0.5F) * 0.2F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    // ------------------------------------------------------------------ save / load

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant().ordinal());
        tag.putInt("Mode", this.getMode().ordinal());
        tag.putInt("Affection", this.getAffection());
        tag.putInt("TreatTicks", this.entityData.get(DATA_TREAT_TICKS));
        tag.put("Pouch", inventory.createTag(this.registryAccess()));
        if (homePos != null) {
            tag.putIntArray("Home", new int[]{homePos.getX(), homePos.getY(), homePos.getZ()});
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(FerretVariant.byId(tag.getInt("Variant")));
        this.entityData.set(DATA_AFFECTION, tag.getInt("Affection"));
        this.entityData.set(DATA_TREAT_TICKS, tag.getInt("TreatTicks"));
        if (tag.contains("Pouch", Tag.TAG_LIST)) {
            inventory.fromTag(tag.getList("Pouch", Tag.TAG_COMPOUND), this.registryAccess());
        }
        FerretMode mode = FerretMode.byId(tag.getInt("Mode"));
        this.entityData.set(DATA_MODE, mode.ordinal());
        int[] home = tag.getIntArray("Home");
        if (mode == FerretMode.WANDER && home.length == 3) {
            this.homePos = new BlockPos(home[0], home[1], home[2]);
            this.restrictTo(this.homePos, 12);
        }
    }
}
