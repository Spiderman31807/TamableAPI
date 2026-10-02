package sxs.api.tamable;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.EnumSet;

public class SitWhenOrderedToGoal extends Goal {
	private final Tamable tamable;
	private final Mob mob;

	public <M extends Mob & Tamable> SitWhenOrderedToGoal(M mob) {
		this.tamable = mob;
		this.mob = mob;
		this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
	}

	@Override
	public boolean canContinueToUse() {
		return this.tamable.isSitting();
	}

	@Override
    public boolean canUse() {
        boolean sitting = this.tamable.isSitting();
        if (!sitting && !this.tamable.isTame())
            return false;
        if (this.mob.isInWater())
            return false;
        if (!this.mob.onGround())
            return false;
            
        LivingEntity owner = this.tamable.getOwner();
        if (owner == null || owner.level() != this.mob.level())
        	return true;
        if(this.mob.distanceToSqr(owner) < 144 && owner.getLastHurtByMob() != null)
        	return false;
        	
        return sitting;
    }

	@Override
	public void start() {
		this.mob.getNavigation().stop();
		this.tamable.setInSittingPose(true);
	}

	@Override
	public void stop() {
		this.tamable.setInSittingPose(false);
	}
}