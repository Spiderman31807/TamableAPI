package sxs.api.tamable;

import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.EnumSet;

public class BreedGoal extends Goal {
	private static final TargetingConditions PARTNER_TARGETING = TargetingConditions.forNonCombat().range(8.0).ignoreLineOfSight();
	private final Class<? extends Mob> partnerClass;
	private final double speedModifier;
	protected final Breedable breedable;
	protected final ServerLevel level;
	protected final Mob mob;
	protected Mob partner;
	private int loveTime;

	public <M extends Mob & Breedable> BreedGoal(M breedable) {
		this.breedable = breedable;
		this.mob = breedable;
		this.level = getServerLevel(breedable);
		this.partnerClass = breedable.getBreedingClass();
		this.speedModifier = breedable.breedSpeed();
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!this.breedable.isInLove())
			return false;

		this.partner = this.getFreePartner();
		return this.partner != null;
	}

	@Override
	public boolean canContinueToUse() {
		return this.partner.isAlive() && this.partner instanceof Breedable breedable && breedable.isInLove() && this.loveTime < 60;
	}

	@Override
	public void stop() {
		this.partner = null;
		this.loveTime = 0;
	}

	@Override
	public void tick() {
		this.mob.getLookControl().setLookAt(this.partner, 10f, this.mob.getMaxHeadXRot());
		this.mob.getNavigation().moveTo(this.partner, this.speedModifier);
		this.loveTime++;
		if (this.loveTime >= this.adjustedTickDelay(60) && this.mob.distanceToSqr(this.partner) < 9)
			this.breed();
	}

	private Mob getFreePartner() {
		List<? extends Mob> list = this.level.getNearbyEntities(this.partnerClass, PARTNER_TARGETING, this.mob, this.mob.getBoundingBox().inflate(8));
		double distance = Double.MAX_VALUE;
		Mob partner = null;
		for (Mob mob : list) {
			if (mob instanceof Breedable breedable && this.breedable.canMate(breedable) && this.mob.distanceToSqr(mob) < distance) {
				distance = this.mob.distanceToSqr(mob);
				partner = mob;
			}
		}

		return partner;
	}

	protected void breed() {
		if (this.partner instanceof Breedable breedablePartner)
			this.breedable.spawnChildFromBreeding(this.level, breedablePartner);
	}
}