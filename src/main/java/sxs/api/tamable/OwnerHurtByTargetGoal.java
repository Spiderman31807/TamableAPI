package sxs.api.tamable;

import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;

import java.util.EnumSet;

public class OwnerHurtByTargetGoal extends TargetGoal {
	private final Tamable tamable;
	private LivingEntity ownerLastHurtBy;
	private int timestamp;

	public <M extends Mob & Tamable> OwnerHurtByTargetGoal(M mob) {
		super(mob, false);
		this.tamable = mob;
		this.setFlags(EnumSet.of(Goal.Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		if (this.tamable.isTame() && !this.tamable.isSitting()) {
			LivingEntity owner = this.tamable.getOwner();
			if (owner == null)
				return false;

			this.ownerLastHurtBy = owner.getLastHurtByMob();
			int timestamp = owner.getLastHurtByMobTimestamp();
			return timestamp != this.timestamp && this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT) && this.tamable.wantsToAttack(this.ownerLastHurtBy, owner);
		}

		return false;
	}

	@Override
	public void start() {
		this.mob.setTarget(this.ownerLastHurtBy);
		LivingEntity owner = this.tamable.getOwner();
		if (owner != null)
			this.timestamp = owner.getLastHurtByMobTimestamp();
		super.start();
	}
}