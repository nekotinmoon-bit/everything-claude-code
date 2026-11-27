package com.cuteferrets.entity.goal;

import com.cuteferrets.entity.FerretEntity;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;

/** Vanilla follow-owner behaviour, active only in modes where the ferret should stay with its owner. */
public class FerretFollowOwnerGoal extends FollowOwnerGoal {
    private final FerretEntity ferret;

    public FerretFollowOwnerGoal(FerretEntity ferret, double speed, float startDistance, float stopDistance) {
        super(ferret, speed, startDistance, stopDistance);
        this.ferret = ferret;
    }

    @Override
    public boolean canUse() {
        return ferret.getMode().followsOwner() && !ferret.isDancing() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return ferret.getMode().followsOwner() && super.canContinueToUse();
    }
}
