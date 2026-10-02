package sxs.api.tamable;

import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;

public class FollowOwnerGoal extends Goal {
	private final Tamable tamable;
	private final PathfinderMob mob;
	private LivingEntity owner;
	private final double speedModifier;
	private final PathNavigation navigation;
	private int timeToRecalcPath;
	private final float stopDistance;
	private final float startDistance;
	private float oldWaterCost;

	public <M extends PathfinderMob & Tamable> FollowOwnerGoal(M mob) {
		this.tamable = mob;
		this.mob = mob;
		this.speedModifier = mob.followOwnerSpeed();
		this.navigation = mob.getNavigation();
		this.startDistance = mob.followOwnerStart();
		this.stopDistance = mob.followOwnerStop();
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		if (!(this.navigation instanceof GroundPathNavigation) && !(this.navigation instanceof FlyingPathNavigation))
			throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
	}

	@Override
	public boolean canUse() {
		LivingEntity owner = this.tamable.getOwner();
		if (owner == null)
			return false;
		if (this.tamable.unableToMoveToOwner())
			return false;
		if (this.mob.distanceToSqr(owner) < this.startDistance * this.startDistance)
			return false;
		this.owner = owner;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.navigation.isDone() || this.tamable.unableToMoveToOwner())
			return false;
		if (this.mob.distanceToSqr(this.owner) <= this.stopDistance * this.stopDistance)
			return false;
		return true;
	}

	@Override
	public void start() {
		this.timeToRecalcPath = 0;
		this.oldWaterCost = this.mob.getPathfindingMalus(PathType.WATER);
		this.mob.setPathfindingMalus(PathType.WATER, 0f);
	}

	@Override
	public void stop() {
		this.owner = null;
		this.navigation.stop();
		this.mob.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
	}

	@Override
	public void tick() {
		boolean shouldTeleport = this.tamable.shouldTryTeleportToOwner();
		if (!shouldTeleport)
			this.mob.getLookControl().setLookAt(this.owner, 10f, this.mob.getMaxHeadXRot());
		if (--this.timeToRecalcPath <= 0) {
			this.timeToRecalcPath = this.adjustedTickDelay(10);
			if (shouldTeleport)
				this.tamable.tryToTeleportToOwner();
			else
				this.navigation.moveTo(this.owner, this.speedModifier);
		}
	}
}