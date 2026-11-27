package com.cuteferrets.entity.goal;

import com.cuteferrets.entity.FerretEntity;
import com.cuteferrets.entity.FerretMode;
import com.cuteferrets.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ore hunt mode: the ferret sniffs the area around it for ores. Ores it can reach (touching a cave or
 * the surface) it runs to and digs out into its pouch. Buried ores it can't reach are reported to the
 * owner with a direction hint, so the player knows where to dig.
 */
public class FerretOreHuntGoal extends Goal {
    private static final int SCAN_INTERVAL = 40;
    private static final int ALERT_INTERVAL = 20 * 20;
    private static final int MOVE_TIMEOUT = 20 * 12;

    private final FerretEntity ferret;
    private final Set<BlockPos> unreachable = new HashSet<>();
    private int scanCooldown = 20;
    private int alertCooldown = 60;
    @Nullable
    private BlockPos target;
    private boolean mining;
    private int ticks;
    private int breakTime;

    public FerretOreHuntGoal(FerretEntity ferret) {
        this.ferret = ferret;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (alertCooldown > 0) {
            alertCooldown--;
        }
        if (ferret.getMode() != FerretMode.ORE || !ferret.canWork()) {
            return false;
        }
        if (scanCooldown-- > 0) {
            return false;
        }
        scanCooldown = SCAN_INTERVAL;
        if (!(ferret.level() instanceof ServerLevel level)) {
            return false;
        }
        if (ferret.getOwner() != null && ferret.distanceToSqr(ferret.getOwner()) > 16 * 16) {
            return false;
        }
        target = scan(level);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && ferret.getMode() == FerretMode.ORE && ferret.canWork()
                && ferret.level().getBlockState(target).is(ModTags.FERRET_ORES)
                && (mining || ticks < MOVE_TIMEOUT);
    }

    @Override
    public void start() {
        mining = false;
        ticks = 0;
        moveToTarget();
    }

    @Override
    public void stop() {
        if (target != null) {
            if (!mining && ticks >= MOVE_TIMEOUT) {
                rememberUnreachable(target);
            }
            ferret.level().destroyBlockProgress(ferret.getId(), target, -1);
        }
        ferret.setDigging(false);
        ferret.getNavigation().stop();
        target = null;
        mining = false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null || !(ferret.level() instanceof ServerLevel level)) {
            return;
        }
        ticks++;
        ferret.getLookControl().setLookAt(Vec3.atCenterOf(target));

        if (!mining) {
            if (isCloseEnough()) {
                mining = true;
                ticks = 0;
                ferret.getNavigation().stop();
                ferret.setDigging(true);
                float hardness = level.getBlockState(target).getDestroySpeed(level, target);
                breakTime = (int) Math.min(160, 20 + Math.max(0, hardness) * 15);
            } else if (ticks % 10 == 0) {
                moveToTarget();
            }
            return;
        }

        BlockState state = level.getBlockState(target);
        level.destroyBlockProgress(ferret.getId(), target, Math.min(9, ticks * 10 / breakTime));
        if (ticks % 4 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.05);
            level.playSound(null, target, state.getSoundType().getHitSound(), SoundSource.NEUTRAL, 0.6F, 1.1F);
        }
        if (ticks >= breakTime) {
            mine(level, target, state);
            target = null;
        }
    }

    private void mine(ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), ferret, tool);
        level.destroyBlock(pos, false, ferret);
        for (ItemStack drop : drops) {
            ferret.celebrateFind(drop.copy());
            ferret.storeOrDrop(drop);
        }
        ExperienceOrb.award(level, Vec3.atCenterOf(pos), 1 + ferret.getRandom().nextInt(3));
        ferret.addAffection(state.is(ModTags.PRECIOUS_ORES) ? 2 : 1);
    }

    private boolean isCloseEnough() {
        Vec3 center = Vec3.atCenterOf(target);
        double dx = center.x - ferret.getX();
        double dz = center.z - ferret.getZ();
        double dy = center.y - (ferret.getY() + 0.3);
        return dx * dx + dz * dz < 2.2 * 2.2 && Math.abs(dy) < 2.0;
    }

    private void moveToTarget() {
        if (target != null) {
            ferret.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.15);
        }
    }

    private void rememberUnreachable(BlockPos pos) {
        if (unreachable.size() > 64) {
            unreachable.clear();
        }
        unreachable.add(pos.immutable());
    }

    // ------------------------------------------------------------------ sniffing

    @Nullable
    private BlockPos scan(ServerLevel level) {
        int radius = ferret.getSniffRadius();
        BlockPos center = ferret.blockPosition();
        List<BlockPos> exposed = new ArrayList<>();
        BlockPos bestBuried = null;
        int bestBuriedScore = Integer.MIN_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -6, -radius), center.offset(radius, 4, radius))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModTags.FERRET_ORES)) {
                continue;
            }
            BlockPos immutable = pos.immutable();
            if (isExposed(level, immutable) && !unreachable.contains(immutable)) {
                exposed.add(immutable);
            } else {
                int score = (state.is(ModTags.PRECIOUS_ORES) ? 1000 : 0) - (int) immutable.distSqr(center);
                if (score > bestBuriedScore) {
                    bestBuriedScore = score;
                    bestBuried = immutable;
                }
            }
        }

        boolean mayDig = ForgeEventFactory.getMobGriefingEvent(level, ferret);
        if (mayDig && !exposed.isEmpty() && ferret.hasFreeSpace()) {
            exposed.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
            for (int i = 0; i < Math.min(5, exposed.size()); i++) {
                BlockPos candidate = exposed.get(i);
                Path path = ferret.getNavigation().createPath(candidate, 1);
                if (path != null && path.canReach()) {
                    return candidate;
                }
                rememberUnreachable(candidate);
            }
        }

        BlockPos alertPos = bestBuried != null ? bestBuried : (exposed.isEmpty() ? null : exposed.get(0));
        if (alertPos != null && alertCooldown <= 0) {
            alertOwner(level, alertPos);
        }
        return null;
    }

    private static boolean isExposed(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos side = pos.relative(direction);
            if (level.getBlockState(side).getCollisionShape(level, side).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void alertOwner(ServerLevel level, BlockPos orePos) {
        alertCooldown = ALERT_INTERVAL;
        BlockState state = level.getBlockState(orePos);
        boolean precious = state.is(ModTags.PRECIOUS_ORES);

        // a little trail of sparkles pointing at the ore
        Vec3 from = ferret.position().add(0, 0.4, 0);
        Vec3 dir = Vec3.atCenterOf(orePos).subtract(from).normalize();
        for (int i = 1; i <= 6; i++) {
            Vec3 p = from.add(dir.scale(i * 0.5));
            level.sendParticles(precious ? ParticleTypes.END_ROD : ParticleTypes.WAX_ON, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        ferret.playSound(precious ? SoundEvents.FOX_SCREECH : SoundEvents.FOX_SNIFF, 0.8F, 1.6F);

        if (ferret.getOwner() instanceof ServerPlayer owner) {
            int dist = (int) Math.round(Math.sqrt(orePos.distSqr(ferret.blockPosition())));
            int dy = orePos.getY() - ferret.blockPosition().getY();
            Component height;
            if (dy < 0) {
                height = Component.translatable("message.cuteferrets.below", -dy);
            } else if (dy > 0) {
                height = Component.translatable("message.cuteferrets.above", dy);
            } else {
                height = Component.translatable("message.cuteferrets.level");
            }
            owner.displayClientMessage(Component.translatable(
                    precious ? "message.cuteferrets.sniffed_precious" : "message.cuteferrets.sniffed",
                    ferret.getDisplayName(), state.getBlock().getName(), dist, height), true);
        }
    }
}
